package org.ejblab.banking.l11;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

import org.ejblab.banking.domain.Account;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Canonical EXTENDED persistence context example.
 *
 * <p>A Stateful bean with {@link PersistenceContextType#EXTENDED EXTENDED}
 * keeps entities managed across multiple method calls. That lets a UI
 * "load, mutate over several requests, save" without re-attaching.
 *
 * <p>RULES (violated at your peril):
 * <ul>
 *   <li>Only Stateful beans may hold an EXTENDED context.</li>
 *   <li>No transaction is active between method calls; changes are
 *       flushed when a method runs inside a tx (default REQUIRED).</li>
 *   <li>Do NOT share the extended EM with another transactional bean
 *       on the same thread — you'll get
 *       {@code "More than one EntityManager in persistence context"}.</li>
 * </ul>
 */
@Stateful
@StatefulTimeout(value = 10, unit = TimeUnit.MINUTES)
public class AccountEditor {

    @PersistenceContext(unitName = "bankingPU", type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    private Account current;

    public Account load(Long accountId) {
        current = em.find(Account.class, accountId);
        return current;
    }

    public void credit(BigDecimal delta) {
        current.setBalance(current.getBalance().add(delta));
        // No explicit persist/merge: `current` stays managed across calls.
    }

    public BigDecimal currentBalance() { return current == null ? null : current.getBalance(); }

    @Remove
    public void save() {
        em.flush();
    }

    @Remove
    public void cancel() {
        em.clear();
    }
}
