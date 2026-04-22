package org.ejblab.banking.l09;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.l09.api.AccountQuery;

import java.math.BigDecimal;
import java.util.List;

/**
 * Server-side implementation of the {@link AccountQuery} remote view.
 *
 * <p>Notes for interview:
 * <ul>
 *   <li>The same bean can expose both a {@code @Remote} and a no-interface
 *       view — simply don't restrict to {@code @Local}.</li>
 *   <li>The portable remote JNDI name follows the pattern
 *       {@code java:jboss/exported/<app>/<module>/<bean>!<iface>}; for a WAR
 *       the app is empty, so clients lookup:
 *       {@code ejb:/banking-lesson-09-remote-ejb-server/AccountQueryBean!org.ejblab.banking.l09.api.AccountQuery}
 *       (with {@code ejb-client} naming).</li>
 * </ul>
 */
@Stateless
public class AccountQueryBean implements AccountQuery {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public List<String> listAccountNumbers() {
        return em.createQuery(
                "select a.accountNumber from Account a order by a.accountNumber",
                String.class).getResultList();
    }

    @Override
    public BigDecimal balanceOf(String accountNumber) {
        var results = em.createQuery(
                "select a.balance from Account a where a.accountNumber = :n",
                BigDecimal.class)
                .setParameter("n", accountNumber)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public String ping(String message) {
        return "pong: " + message;
    }
}
