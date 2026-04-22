package org.ejblab.banking.l04;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-key token-bucket rate limiter.
 *
 * <p>{@code BEAN}-managed concurrency + atomic updates on
 * {@link ConcurrentMap}. This is a textbook example of why EJB container
 * locking sometimes is NOT what you want: we want independent keys to
 * proceed in parallel without blocking each other.
 *
 * <p>Semantics: each {@code key} gets {@code capacity} tokens; tokens
 * refill at {@code refillPerSecond}. {@link #tryAcquire(String)} returns
 * true iff at least one token was available at call time.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class RateLimiterSingleton {

    /** configuration baked in for the lesson; production would read these from config. */
    private static final long CAPACITY_TOKENS = 5;
    private static final double REFILL_PER_SECOND = 5.0;

    private record Bucket(double tokens, long lastRefillNanos) {}

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        long now = System.nanoTime();
        AtomicBoolean granted = new AtomicBoolean(false);
        buckets.compute(key, (k, cur) -> {
            double tokens;
            if (cur == null) {
                tokens = CAPACITY_TOKENS;
            } else {
                double elapsed = (now - cur.lastRefillNanos) / 1e9;
                tokens = Math.min(CAPACITY_TOKENS, cur.tokens + elapsed * REFILL_PER_SECOND);
            }
            if (tokens >= 1.0) {
                granted.set(true);
                return new Bucket(tokens - 1.0, now);
            }
            return new Bucket(tokens, now);
        });
        return granted.get();
    }

    public double availableTokens(String key) {
        Bucket b = buckets.get(key);
        if (b == null) return CAPACITY_TOKENS;
        double elapsed = (System.nanoTime() - b.lastRefillNanos) / 1e9;
        return Math.min(CAPACITY_TOKENS, b.tokens + elapsed * REFILL_PER_SECOND);
    }
}
