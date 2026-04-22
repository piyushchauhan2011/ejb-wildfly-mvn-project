package org.ejblab.banking.l11;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.ejblab.banking.domain.Account;

import java.math.BigDecimal;

/**
 * Demonstrates:
 * <ul>
 *   <li>Method-level Bean Validation on EJB parameters ({@link Valid}).</li>
 *   <li>Optimistic locking via {@code @Version} on {@link Account}: two
 *       concurrent updates will cause one to throw {@code OptimisticLockException}.</li>
 *   <li>Automatic retry on that exception via {@link Retryable} interceptor.</li>
 * </ul>
 */
@Stateless
public class AdjustmentService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Retryable(maxAttempts = 5, backoffMillis = 10)
    public BigDecimal adjust(@NotNull String accountNumber, @Valid @NotNull TransferCommand cmd) {
        // Intentionally DO NOT use pessimistic locking — we rely on @Version for
        // optimistic concurrency, and RetryInterceptor to survive collisions.
        Account a = em.createQuery("from Account a where a.accountNumber = :n", Account.class)
                .setParameter("n", accountNumber)
                .getSingleResult();
        a.setBalance(a.getBalance().add(cmd.amount()));
        em.flush(); // force version bump + detect conflicts inside this tx
        return a.getBalance();
    }
}
