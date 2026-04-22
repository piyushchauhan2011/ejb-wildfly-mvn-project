package org.ejblab.banking.l10;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferRequest;

/**
 * Stage 1 in the E2E flow. The servlet calls {@code submit(...)} which:
 * <ol>
 *   <li>persists a PENDING {@link Transfer} row (idempotency anchor)</li>
 *   <li>publishes a message to {@code TransfersRequested} (XA so the DB
 *       row and the message commit atomically)</li>
 *   <li>returns 202 Accepted</li>
 * </ol>
 */
@Stateless
public class TransferFacade {

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @Resource(lookup = "java:/JmsXA")
    ConnectionFactory connectionFactory;

    @Resource(lookup = "java:/jms/queue/TransfersRequested")
    Queue requested;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transfer submit(TransferRequest req) {
        // Idempotency: if we've seen this request before, return the existing row.
        var existing = em.createQuery(
                "select t from Transfer t where t.clientRequestId = :c", Transfer.class)
                .setParameter("c", req.clientRequestId())
                .getResultList();
        if (!existing.isEmpty()) return existing.get(0);

        Account from = findByNumber(req.fromAccountNumber());
        Account to = findByNumber(req.toAccountNumber());

        Transfer t = new Transfer(from, to, req.amount());
        t.setClientRequestId(req.clientRequestId());
        // status defaults to PENDING on the entity
        em.persist(t);
        em.flush();

        try (JMSContext ctx = connectionFactory.createContext()) {
            ctx.createProducer().send(requested, req);
        }
        return t;
    }

    private Account findByNumber(String number) {
        return em.createQuery("select a from Account a where a.accountNumber = :n", Account.class)
                .setParameter("n", number).getSingleResult();
    }
}
