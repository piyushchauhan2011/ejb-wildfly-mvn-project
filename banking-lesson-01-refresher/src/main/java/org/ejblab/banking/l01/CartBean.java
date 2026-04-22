package org.ejblab.banking.l01;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tiny demo {@code @Stateful} session bean. Not banking-domain specific so you
 * can focus on the lifecycle.
 *
 * <p>Key facts to remember:
 * <ul>
 *   <li>One instance per client. The client holds a proxy that routes every
 *       call back to the same underlying instance.</li>
 *   <li>The container may passivate the instance (serialize non-transient
 *       state to disk) if idle - that's why fields must be
 *       {@link java.io.Serializable}.</li>
 *   <li>Methods annotated {@link Remove} terminate the conversation; the
 *       container destroys the instance and frees resources.</li>
 *   <li>{@link StatefulTimeout} ensures abandoned sessions don't leak; the
 *       container removes them after the timeout.</li>
 * </ul>
 *
 * <p>Interview answer: "Why use this over CDI {@code @SessionScoped}?"
 *   - Stateful beans give you EJB services (TX, security, timers, concurrency
 *     control, passivation). CDI session scope is servlet-session bound and
 *     doesn't automatically propagate over remote EJB or have @Remove.
 */
@Stateful
@StatefulTimeout(value = 10, unit = TimeUnit.MINUTES)
public class CartBean {

    private final List<String> items = new ArrayList<>();

    public void add(String sku) { items.add(sku); }

    public List<String> items() { return Collections.unmodifiableList(items); }

    public int size() { return items.size(); }

    /** Ends the conversation; container destroys this instance after return. */
    @Remove
    public List<String> checkout() {
        List<String> snapshot = List.copyOf(items);
        items.clear();
        return snapshot;
    }
}
