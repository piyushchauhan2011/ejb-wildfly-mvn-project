package org.ejblab.banking.l11;

import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Classic Stateful bean demonstrating passivation and {@link Remove}.
 *
 * <p>{@link StatefulTimeout} evicts idle sessions; {@link PrePassivate} /
 * {@link PostActivate} let us release non-serializable resources (none here;
 * shown for form). {@code @Remove} lets the client end the conversation
 * explicitly — crucial because WildFly's SFSB cache, not the client, owns
 * the lifecycle.
 */
@Stateful
@SessionScoped
@StatefulTimeout(value = 15, unit = TimeUnit.MINUTES)
public class BasketBean implements Serializable {

    private static final Logger log = Logger.getLogger(BasketBean.class.getName());

    private final List<String> items = new ArrayList<>();

    public void add(String sku) { items.add(sku); }

    public List<String> items() { return Collections.unmodifiableList(items); }

    @PrePassivate
    void passivate() {
        // Flush caches, close non-serializable fields. All state must be serializable at this point.
        log.info("passivating basket with " + items.size() + " item(s)");
    }

    @PostActivate
    void activate() {
        log.info("activated basket with " + items.size() + " item(s)");
    }

    @Remove
    public void checkout() {
        log.info("checkout: " + items);
        // The EJB is destroyed right after this method returns.
    }
}
