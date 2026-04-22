package org.ejblab.banking.l11;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a business method that should transparently retry when an
 * optimistic lock conflict ({@link jakarta.persistence.OptimisticLockException})
 * occurs. Paired with {@link RetryInterceptor}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Retryable {
    /** Maximum number of attempts including the first one. */
    int maxAttempts() default 3;

    /** Backoff in milliseconds; multiplied by attempt number for a simple linear backoff. */
    long backoffMillis() default 20L;
}
