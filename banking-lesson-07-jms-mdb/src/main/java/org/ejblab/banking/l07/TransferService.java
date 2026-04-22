package org.ejblab.banking.l07;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.LedgerEntry;
import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferRequest;

/** Sync path used by the sync-vs-async benchmark. */
@Stateless
public class TransferService {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transfer transfer(TransferRequest req) {
        // Idempotency: if we've already completed this clientRequestId, return it.
        Transfer existing = em.createQuery(
                "SELECT t FROM Transfer t WHERE t.clientRequestId = :c", Transfer.class)
                .setParameter("c", req.clientRequestId())
                .getResultStream().findFirst().orElse(null);
        if (existing != null) return existing;

        Account src = lookupForUpdate(req.fromAccountNumber());
        Account dst = lookupForUpdate(req.toAccountNumber());
        src.debit(req.amount());
        dst.credit(req.amount());

        Transfer t = new Transfer(src, dst, req.amount());
        t.setClientRequestId(req.clientRequestId());
        em.persist(t);
        em.persist(new LedgerEntry(src, t, LedgerEntry.Direction.DEBIT,  req.amount()));
        em.persist(new LedgerEntry(dst, t, LedgerEntry.Direction.CREDIT, req.amount()));
        t.markCompleted();
        return t;
    }

    private Account lookupForUpdate(String accountNumber) {
        return em.createQuery(
                "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                .setParameter("n", accountNumber)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
    }
}
