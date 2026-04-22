package org.ejblab.banking.l11;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(ArquillianExtension.class)
public class Lesson11IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "banking-lesson-11-advanced-patterns.war")
                .addPackages(true, "org.ejblab.banking.l11", "org.ejblab.banking.domain")
                .addAsWebInfResource("beans.xml", "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve("org.flywaydb:flyway-core", "org.flywaydb:flyway-database-postgresql")
                        .withTransitivity().asFile());
    }

    @Inject AdjustmentService adj;

    @Test
    void validAdjustSucceeds() {
        BigDecimal result = adj.adjust("ADV-001", new TransferCommand("ADV-001", "ADV-002", new BigDecimal("1.00")));
        assertNotNull(result);
    }

    @Test
    void invalidAmountViolatesConstraint() {
        // The EJB wraps ConstraintViolationException (which is not cleanly
        // Serializable because it carries back-references to the bean) in an
        // EJBException. We just verify that the invocation fails.
        EJBException ex = assertThrows(EJBException.class, () ->
                adj.adjust("ADV-001", new TransferCommand("ADV-001", "ADV-002", new BigDecimal("-1.00"))));
        assertNotNull(ex);
    }
}
