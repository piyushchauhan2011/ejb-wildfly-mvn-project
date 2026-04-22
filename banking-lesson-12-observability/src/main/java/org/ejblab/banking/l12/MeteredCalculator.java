package org.ejblab.banking.l12;

import jakarta.ejb.Stateless;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stateless bean instrumented with MicroProfile Metrics.
 *
 * <ul>
 *   <li>{@link Counted} bumps a counter every call — available at
 *       {@code GET /metrics} with name {@code banking_interest_calc_total}.</li>
 *   <li>{@link Timed} captures p50/p95/p99 latencies.</li>
 * </ul>
 *
 * <p>All metric names include the application's {@code @ApplicationScoped}
 * bean context. Prometheus scrape: {@code /metrics} returns OpenMetrics text.
 */
@Stateless
public class MeteredCalculator {

    @Counted(name = "banking_interest_calc_total",
             description = "Total interest calculations performed")
    @Timed(name = "banking_interest_calc_duration",
           description = "Interest calculation duration")
    public BigDecimal accrue(BigDecimal balance, BigDecimal annualRate, int days) {
        BigDecimal daily = annualRate.divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(daily);
        BigDecimal result = balance;
        for (int i = 0; i < days; i++) {
            result = result.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }
        return result;
    }
}
