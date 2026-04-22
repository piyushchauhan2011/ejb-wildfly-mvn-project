package org.ejblab.banking.l05;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class Lesson05IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson05-it.war")
                .addPackages(true, "org.ejblab.banking.l05")
                .addAsWebInfResource("beans.xml", "beans.xml");
    }

    @Inject QuoteService quotes;
    @Inject AuditTrail trail;

    @BeforeEach
    void clear() { trail.clear(); }

    @Test
    void audit_and_timed_interceptors_fire_on_success() {
        BigDecimal fee = quotes.quoteTransferFee(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("0.35"), fee);
        // 1 audit entry recorded
        assertEquals(1, trail.all().size());
        assertEquals("OK", trail.all().get(0).outcome());
    }

    @Test
    void audit_captures_thrown_exceptions() {
        assertThrows(IllegalStateException.class, quotes::quoteBroken);
        assertEquals(1, trail.all().size());
        assertEquals("ERR", trail.all().get(0).outcome());
        assertNotNull(trail.all().get(0).error());
    }
}
