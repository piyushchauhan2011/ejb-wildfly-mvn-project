package org.ejblab.banking.l10;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.LedgerEntry;
import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferRequest;
import org.ejblab.banking.domain.TransferStatus;
import org.ejblab.banking.domain.InsufficientFundsException;

/**
 * Stage 2 in the E2E flow, driven by the MDB. Performs the balance move
 * in a single XA transaction: JMS ack + JDBC row updates + JDBC ledger
 * inserts + JMS send to {@code TransfersCompleted} all commit or all roll
 * back together.
 */
@Stateless
public class TransferProcessor {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @Resource(lookup = "java:/JmsXA")
    ConnectionFactory cf;

    @Resource(lookup = "java:/jms/queue/TransfersCompleted")
    Queue completed;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void process(TransferRequest req) {
        Transfer t = em.createQuery(
                "select t from Transfer t where t.clientRequestId = :c", Transfer.class)
                .setParameter("c", req.clientRequestId())
                .getSingleResult();

        if (t.getStatus() != TransferStatus.PENDING) {
            return; // idempotent retry: already moved this intent
        }

        try {
            Account from = em.find(Account.class, t.getFromAccount().getId(), LockModeType.PESSIMISTIC_WRITE);
            Account to = em.find(Account.class, t.getToAccount().getId(), LockModeType.PESSIMISTIC_WRITE);

            if (from.getBalance().compareTo(req.amount()) < 0) {
                throw new InsufficientFundsException("insufficient balance");
            }

            from.setBalance(from.getBalance().subtract(req.amount()));
            to.setBalance(to.getBalance().add(req.amount()));

            em.persist(new LedgerEntry(from, t, LedgerEntry.Direction.DEBIT, req.amount()));
            em.persist(new LedgerEntry(to, t, LedgerEntry.Direction.CREDIT, req.amount()));

            t.markCompleted();
        } catch (InsufficientFundsException e) {
            t.markFailed(e.getMessage());
        }

        try (JMSContext ctx = cf.createContext()) {
            ctx.createProducer().send(completed, req);
        }
    }
}
