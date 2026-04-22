package org.ejblab.banking.l05;

import jakarta.annotation.Priority;
import jakarta.ejb.Timeout;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.logging.Logger;

/**
 * Measures method (and EJB timer callback) duration.
 *
 * <p>{@link AroundTimeout} is the interceptor hook for EJB timer callbacks
 * ({@link Timeout} or {@code @Schedule}). Without it, your audit/timing
 * interceptors would skip timer invocations - a very common cross-cutting
 * blind spot. Try Lesson 6 for the timers this applies to.
 */
@Timed
@Interceptor
@Priority(3100)
public class TimedInterceptor {

    private static final Logger log = Logger.getLogger("TIMED");

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        return timeIt("invoke", ctx);
    }

    @AroundTimeout
    public Object aroundTimer(InvocationContext ctx) throws Exception {
        return timeIt("timeout", ctx);
    }

    private Object timeIt(String phase, InvocationContext ctx) throws Exception {
        long t0 = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            long us = (System.nanoTime() - t0) / 1_000;
            log.info(phase + " " + ctx.getMethod().getDeclaringClass().getSimpleName()
                    + "." + ctx.getMethod().getName() + " took " + us + " us");
        }
    }
}
