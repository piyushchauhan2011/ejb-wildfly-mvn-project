package org.ejblab.banking.l02;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;

import java.util.List;
import java.util.Optional;

/**
 * Stateless repository for {@link Account}.
 *
 * <p>Notice two details that matter for later lessons:
 * <ul>
 *   <li>{@link #findByNumberForUpdate(String)} uses
 *       {@link LockModeType#PESSIMISTIC_WRITE}. Lesson 3 contrasts this with
 *       optimistic locking via {@code @Version}.</li>
 *   <li>The class-level default {@code @TransactionAttribute} is
 *       {@code REQUIRED} - we rely on the caller's transaction if one
 *       exists, otherwise the container starts a new one for each method.</li>
 * </ul>
 */
@Stateless
public class AccountRepository {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    public Account save(Account a) {
        if (a.getId() == null) em.persist(a); else a = em.merge(a);
        return a;
    }

    public Optional<Account> findByNumber(String accountNumber) {
        try {
            return Optional.of(em.createQuery(
                    "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                    .setParameter("n", accountNumber)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Used from Lesson 3's {@code TransferService} to prevent
     * lost-update anomalies via a row lock.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public Account findByNumberForUpdate(String accountNumber) {
        return em.createQuery(
                "SELECT a FROM Account a WHERE a.accountNumber = :n", Account.class)
                .setParameter("n", accountNumber)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
    }

    public List<Account> findByCustomer(Long customerId) {
        return em.createQuery(
                "SELECT a FROM Account a WHERE a.owner.id = :c ORDER BY a.id", Account.class)
                .setParameter("c", customerId)
                .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(a) FROM Account a", Long.class).getSingleResult();
    }
}
