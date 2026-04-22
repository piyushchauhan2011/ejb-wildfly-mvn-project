package org.ejblab.banking.l01;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * Classic {@code @Stateless} session bean with the <em>no-interface view</em>
 * ({@link LocalBean} is implicit when no business interface is declared).
 *
 * <p>What to notice:
 * <ul>
 *   <li>No synchronization needed: the container serializes calls per-instance
 *       from the pool. Each concurrent caller gets its own instance.</li>
 *   <li>No fields mean no state; any field you add here is per-instance and
 *       survives between calls but is NOT shared across callers.</li>
 *   <li>{@code @LocalBean} can be omitted; it's shown for clarity.</li>
 * </ul>
 */
@Stateless
@LocalBean
public class GreetingBean {

    public String hello(String name) {
        String safe = (name == null || name.isBlank()) ? "world" : name;
        return "Hello, " + safe + "!";
    }
}
