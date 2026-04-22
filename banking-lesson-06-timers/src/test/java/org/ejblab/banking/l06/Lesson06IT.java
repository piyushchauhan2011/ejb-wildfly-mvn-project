package org.ejblab.banking.l06;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;
import org.ejblab.banking.domain.Customer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class Lesson06IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson06-it.war")
                .addPackages(true, "org.ejblab.banking.domain")
                .addPackages(true, "org.ejblab.banking.l06")
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
    @Inject jakarta.transaction.UserTransaction utx;
    @Inject NightlyInterestBean job;

    @Test
    void runOnce_is_idempotent_on_same_date() throws Exception {
        LocalDate today = LocalDate.now();

        // Seed inside a user-managed transaction. Arquillian tests don't run
        // inside an ambient TX, so raw em.persist() would fail with
        // TransactionRequiredException.
        utx.begin();
        Customer c = new Customer("Timers Test " + UUID.randomUUID(),
                "timers-" + UUID.randomUUID() + "@example.com");
        Account a = new Account(
                "TMR" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
                c, AccountType.SAVINGS, new BigDecimal("10000.00"));
        em.persist(c);
        em.persist(a);
        em.createQuery("DELETE FROM InterestRun r WHERE r.runDate = :d")
                .setParameter("d", today).executeUpdate();
        utx.commit();

        job.runOnce(today);  // 1st run does work
        job.runOnce(today);  // 2nd run for same date: no-op (idempotent)

        utx.begin();
        Long runs = em.createQuery(
                "SELECT COUNT(r) FROM InterestRun r WHERE r.runDate = :d", Long.class)
                .setParameter("d", today).getSingleResult();
        utx.commit();
        assertEquals(1L, runs);
    }
}
