package org.ejblab.banking.l05;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binding for {@link TimedInterceptor}. Demonstrates stacking multiple
 * interceptors: a method annotated {@code @Audited @Timed} runs through
 * both, ordered by {@code @Priority}.
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Timed {}
