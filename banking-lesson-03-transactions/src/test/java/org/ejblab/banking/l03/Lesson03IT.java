package org.ejblab.banking.l03;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.InsufficientFundsException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class Lesson03IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson03-it.war")
                .addPackages(true, "org.ejblab.banking.domain")
                .addPackages(true, "org.ejblab.banking.l03")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsWebInfResource("beans.xml", "beans.xml")
                .addAsLibraries(
                        Maven.resolver().resolve("org.flywaydb:flyway-core:10.20.1")
                                .withTransitivity().asFile())
                .addAsLibraries(
                        Maven.resolver().resolve("org.flywaydb:flyway-database-postgresql:10.20.1")
                                .withTransitivity().asFile());
    }

    @PersistenceContext(unitName = "bankingPU") EntityManager em;
    @Inject SeedBean seed;
    @Inject TransferService cmt;
    @Inject AccountRepository accounts;

    @BeforeEach
    void setUp() {
        seed.seedIfMissing();
        seed.reset();
    }

    @Test
    void cmt_transfer_commits_both_sides() {
        cmt.transfer("ACC-001", "ACC-002", new BigDecimal("100.00"));
        assertEquals(new BigDecimal("900.00"),
                accounts.findByNumber("ACC-001").orElseThrow().getBalance());
        assertEquals(new BigDecimal("1100.00"),
                accounts.findByNumber("ACC-002").orElseThrow().getBalance());
    }

    @Test
    void insufficient_funds_rolls_back_everything() {
        assertThrows(InsufficientFundsException.class,
                () -> cmt.transfer("ACC-001", "ACC-002", new BigDecimal("99999.00")));

        // @ApplicationException(rollback=true) on InsufficientFundsException
        // guarantees that NOT A SINGLE side-effect of the method landed.
        assertEquals(new BigDecimal("1000.00"),
                accounts.findByNumber("ACC-001").orElseThrow().getBalance());
        assertEquals(new BigDecimal("1000.00"),
                accounts.findByNumber("ACC-002").orElseThrow().getBalance());
    }
}
