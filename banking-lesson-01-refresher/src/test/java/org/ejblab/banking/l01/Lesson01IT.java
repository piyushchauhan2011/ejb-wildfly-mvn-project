package org.ejblab.banking.l01;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Arquillian smoke test: deploys a minimal WAR into the managed WildFly
 * container and verifies the three bean types can be injected and called.
 *
 * <p>Run with: {@code mvn -Pit verify}
 */
@ExtendWith(ArquillianExtension.class)
class Lesson01IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson01-it.war")
                .addClasses(GreetingBean.class, CartBean.class, AccountCatalogSingleton.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    GreetingBean greeting;

    @Inject
    AccountCatalogSingleton catalog;

    @Inject
    CartBean cart;

    @Test
    void statelessBean_returns_greeting() {
        assertEquals("Hello, Grace!", greeting.hello("Grace"));
    }

    @Test
    void singleton_is_seeded_on_startup() {
        assertEquals(3, catalog.size());
        assertTrue(catalog.ownerOf("GB29NWBK60161331926819").isPresent());
    }

    @Test
    void stateful_bean_retains_state_within_one_reference() {
        cart.add("SKU-1");
        cart.add("SKU-2");
        assertEquals(2, cart.size());
        // checkout() is @Remove: it returns the contents and the container
        // destroys this SFSB afterwards, so any further call on `cart` would
        // throw NoSuchEJBException. That's by design for stateful lifecycles.
        var checked = cart.checkout();
        assertEquals(2, checked.size());
        assertThrows(jakarta.ejb.NoSuchEJBException.class, cart::size,
                "SFSB should be gone after @Remove");
    }
}
