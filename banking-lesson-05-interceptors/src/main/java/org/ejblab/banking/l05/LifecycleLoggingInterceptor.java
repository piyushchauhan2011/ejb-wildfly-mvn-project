package org.ejblab.banking.l05;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

import java.util.logging.Logger;

/**
 * Class-level interceptor (not CDI-binding). Demonstrates {@link AroundConstruct},
 * which wraps the bean's constructor + {@code @PostConstruct}.
 *
 * <p>Use {@code @Interceptors(LifecycleLoggingInterceptor.class)} on a bean
 * class to apply it. This is the older-style activation mechanism and
 * lives alongside CDI {@code @InterceptorBinding}.
 */
public class LifecycleLoggingInterceptor {

    private static final Logger log = Logger.getLogger("LIFECYCLE");

    @AroundConstruct
    public void log(InvocationContext ctx) throws Exception {
        Class<?> target = ctx.getConstructor().getDeclaringClass();
        log.info("constructing " + target.getSimpleName());
        ctx.proceed();
        log.info("constructed  " + target.getSimpleName());
    }
}
