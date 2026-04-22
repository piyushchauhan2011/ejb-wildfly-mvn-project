package org.ejblab.banking.l11;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.OptimisticLockException;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retries {@link Retryable}-annotated business methods when an
 * {@link OptimisticLockException} (or its SQL/Hibernate equivalents)
 * is thrown.
 *
 * <p>The interceptor is transaction-scoped: because it sits OUTSIDE the
 * bean's transactional interceptor chain (thanks to {@link Priority}
 * {@code Interceptor.Priority.APPLICATION}), each retry attempt opens a
 * fresh transaction if the target method is {@code REQUIRED}.
 */
@Retryable
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RetryInterceptor {

    private static final Logger log = Logger.getLogger(RetryInterceptor.class.getName());

    @AroundInvoke
    public Object retry(InvocationContext ctx) throws Exception {
        Retryable cfg = readConfig(ctx.getMethod());
        int attempts = Math.max(1, cfg.maxAttempts());
        long backoff = Math.max(0, cfg.backoffMillis());

        Exception last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return ctx.proceed();
            } catch (OptimisticLockException | jakarta.ejb.EJBTransactionRolledbackException e) {
                last = e;
                if (!isOptimistic(e) || attempt == attempts) throw e;
                log.log(Level.WARNING, "Optimistic conflict on " + ctx.getMethod().getName()
                        + " attempt " + attempt + "/" + attempts + " — retrying");
                Thread.sleep(backoff * attempt);
            }
        }
        throw last;
    }

    private boolean isOptimistic(Throwable t) {
        while (t != null) {
            if (t instanceof OptimisticLockException) return true;
            t = t.getCause();
        }
        return false;
    }

    private Retryable readConfig(Method m) {
        Retryable onMethod = m.getAnnotation(Retryable.class);
        if (onMethod != null) return onMethod;
        return m.getDeclaringClass().getAnnotation(Retryable.class);
    }
}
