package org.ejblab.banking.l05;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CDI {@link InterceptorBinding} that triggers {@link AuditInterceptor}.
 *
 * <p>{@code @InterceptorBinding} is the modern, CDI-native way to
 * associate an interceptor with target code. It beats the older
 * {@code @Interceptors(...)} annotation on EJBs because:
 * <ul>
 *   <li>The binding is discoverable and composable.</li>
 *   <li>Interceptor activation / ordering is controlled via
 *       {@code beans.xml} {@code <interceptors>} section.</li>
 *   <li>It works on plain CDI beans, not just EJBs.</li>
 * </ul>
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Audited {
    /**
     * Human-readable category; written into the audit record.
     * {@link Nonbinding} so any value on the target still matches the
     * interceptor's bare {@code @Audited} binding.
     */
    @Nonbinding String value() default "";
}
