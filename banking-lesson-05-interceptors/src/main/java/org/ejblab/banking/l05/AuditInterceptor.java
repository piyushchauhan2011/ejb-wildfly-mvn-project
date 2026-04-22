package org.ejblab.banking.l05;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Handler for {@link Audited}. Records method name, args, outcome, duration.
 *
 * <p>{@link Priority} determines ordering among multiple interceptors bound
 * to the same method. Jakarta defines ranges:
 * <ul>
 *   <li>{@code 1000-1999} - platform-level (transactions, security)</li>
 *   <li>{@code 2000-2999} - library</li>
 *   <li>{@code 3000+} - application</li>
 * </ul>
 * So our audit interceptor sits at {@code 3000} - runs AFTER tx/security
 * on the way in and BEFORE them on the way out, which is almost always
 * what you want for audit logs.
 */
@Audited
@Interceptor
@Priority(3000)
public class AuditInterceptor {

    private static final Logger log = Logger.getLogger("AUDIT");

    @Inject
    AuditTrail trail;

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        String target = ctx.getMethod().getDeclaringClass().getSimpleName()
                + "." + ctx.getMethod().getName();
        String args = safeArgs(ctx.getParameters());
        long t0 = System.nanoTime();
        try {
            Object result = ctx.proceed();
            long us = (System.nanoTime() - t0) / 1_000;
            trail.record(new AuditTrail.Entry(Instant.now(), target, args, "OK", us, null));
            return result;
        } catch (Throwable t) {
            long us = (System.nanoTime() - t0) / 1_000;
            trail.record(new AuditTrail.Entry(Instant.now(), target, args, "ERR", us,
                    t.getClass().getSimpleName() + ": " + t.getMessage()));
            throw t;
        }
    }

    private static String safeArgs(Object[] args) {
        if (args == null) return "[]";
        // Naive redaction: passwords, tokens, full account numbers.
        // Lesson 8 (security) expands this with a proper masking strategy.
        return Arrays.toString(args);
    }
}
