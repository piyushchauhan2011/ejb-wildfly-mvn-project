package org.ejblab.banking.l12;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ArquillianExtension.class)
public class Lesson12IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "banking-lesson-12-observability.war")
                .addPackages(true, "org.ejblab.banking.l12", "org.ejblab.banking.domain")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve("org.flywaydb:flyway-core", "org.flywaydb:flyway-database-postgresql")
                        .withTransitivity().asFile());
    }

    @Inject MeteredCalculator calc;

    @Test
    void computes() {
        BigDecimal result = calc.accrue(new BigDecimal("1000.00"), new BigDecimal("0.05"), 30);
        assertTrue(result.compareTo(new BigDecimal("1000.00")) > 0);
    }
}
