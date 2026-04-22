package org.ejblab.banking.l05;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Target bean wrapped by several interceptors.
 *
 * <p>Stacking: {@code @Audited} + {@code @Timed} means every call is both
 * audited and timed. Method-level annotations add to class-level ones.
 *
 * <p>{@code @Interceptors(LifecycleLoggingInterceptor.class)} at class
 * level additionally hooks construction.
 */
@Stateless
@Audited("quotes")
@Timed
@Interceptors(LifecycleLoggingInterceptor.class)
public class QuoteService {

    public BigDecimal quoteTransferFee(BigDecimal amount) {
        // Fake fee: 0.25% + flat 0.10
        return amount.multiply(new BigDecimal("0.0025"))
                .add(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Method-level {@code @Audited("fx-rate")} overrides class-level category. */
    @Audited("fx-rate")
    public BigDecimal quoteFxRate(String from, String to) {
        // Dummy rate, deterministic per-pair for the benchmark.
        double delta = (from.hashCode() - to.hashCode()) / 1e6;
        return BigDecimal.valueOf(1.0 + Math.abs(delta)).setScale(6, RoundingMode.HALF_UP);
    }

    /** Thrown-into-interceptor demonstration. */
    public BigDecimal quoteBroken() {
        throw new IllegalStateException("broken on purpose");
    }
}
