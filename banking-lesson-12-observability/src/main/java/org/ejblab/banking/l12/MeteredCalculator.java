package org.ejblab.banking.l12;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stateless bean instrumented with Micrometer — the metrics backend WildFly
 * ships with since 28+, replacing the older MicroProfile Metrics subsystem.
 *
 * <ul>
 *   <li>{@link Counted} bumps a counter every call — available at
 *       {@code GET /metrics} with name {@code banking_interest_calc_total}.</li>
 *   <li>{@link Timed} captures p50/p95/p99 latencies.</li>
 * </ul>
 *
 * <p>WildFly's {@code micrometer} subsystem auto-discovers these annotations
 * on EJB/CDI beans when the {@code micrometer} Galleon layer is provisioned.
 * Prometheus scrape path depends on the configured registry (by default
 * OTLP, but can be switched to Prometheus via CLI).
 */
@Stateless
public class MeteredCalculator {

    @Counted(value = "banking_interest_calc_total",
             description = "Total interest calculations performed")
    @Timed(value = "banking_interest_calc_duration",
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
