package org.ejblab.banking.l03;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;

import java.util.Optional;

@Stateless
public class AccountRepository {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    public Optional<Account> findByNumber(String accountNumber) {
        try {
            return Optional.of(em.createQuery(
                    "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                    .setParameter("n", accountNumber)
                    .getSingleResult());
        } catch (NoResultException e) { return Optional.empty(); }
    }

    /**
     * Must run inside an existing transaction - we want the row lock to be
     * visible to the caller's transaction.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public Account findByNumberForUpdate(String accountNumber) {
        return em.createQuery(
                "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                .setParameter("n", accountNumber)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
    }

    public Account save(Account a) {
        if (a.getId() == null) em.persist(a); else a = em.merge(a);
        return a;
    }
}
