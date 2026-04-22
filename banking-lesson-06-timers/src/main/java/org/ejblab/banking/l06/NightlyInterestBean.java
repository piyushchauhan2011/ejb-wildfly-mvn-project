package org.ejblab.banking.l06;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Declarative schedule: nightly at 00:15 server time, accrue interest on
 * every SAVINGS account at the configured rate.
 *
 * <p>Important flags:
 * <ul>
 *   <li>{@code persistent=true} (default) = timer survives restart and is
 *       visible cluster-wide.</li>
 *   <li>{@code info="..."} = opaque string you can read via
 *       {@link Timer#getInfo()} in programmatic timers.</li>
 * </ul>
 *
 * <p>Idempotency: every fire starts by trying to INSERT a row into
 * {@code interest_runs} for today. If we already ran today, the insert
 * fails (primary-key conflict) and we bail out. Without this, a flaky
 * timer (missed or duplicated fires after a node crash) would double-pay.
 */
@Singleton
public class NightlyInterestBean {

    private static final Logger log = Logger.getLogger(NightlyInterestBean.class.getName());

    private static final BigDecimal DAILY_RATE = new BigDecimal("0.00006") /* ~2.25% APR / 365 */;

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @Resource SessionContext ctx;

    private Clock clock = Clock.systemDefaultZone();

    @Schedule(hour = "0", minute = "15", second = "0",
              persistent = true, info = "nightly-interest")
    public void nightlyInterest() {
        // Re-enter through the proxy so the job runs in REQUIRES_NEW, giving us
        // per-batch commits and per-batch idempotency.
        ctx.getBusinessObject(NightlyInterestBean.class).runOnce(LocalDate.now(clock));
    }

    /**
     * Package-visible so {@code OnDemandTimerServlet} can trigger the same
     * job on demand (useful in tests and in prod "run it now" buttons).
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void runOnce(LocalDate day) {
        // Idempotency check: if a row already exists, bail early. No writes in
        // this TX yet, so nothing to roll back.
        InterestRun existing = em.find(InterestRun.class, day);
        if (existing != null) {
            log.info("interest run for " + day + " already exists; skipping");
            return;
        }
        InterestRun run = new InterestRun(day);
        em.persist(run);
        em.flush();   // surface PK conflicts here (another node won the race)

        List<Account> savings = em.createQuery(
                "SELECT a FROM Account a WHERE a.type = :t", Account.class)
                .setParameter("t", AccountType.SAVINGS)
                .getResultList();

        BigDecimal total = BigDecimal.ZERO;
        int n = 0;
        for (Account a : savings) {
            BigDecimal accrued = a.getBalance().multiply(DAILY_RATE)
                    .setScale(2, RoundingMode.HALF_EVEN);
            if (accrued.signum() > 0) {
                a.credit(accrued);
                total = total.add(accrued);
                n++;
            }
            if (n % 500 == 0) em.flush();
        }
        run.complete(n, total);
        log.info("interest run complete: " + n + " accounts, total=" + total);
    }
}
