package org.ejblab.banking.l02;

import jakarta.inject.Inject;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class Lesson02IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson02-it.war")
                .addPackages(true, "org.ejblab.banking.domain")
                .addPackages(true, "org.ejblab.banking.l02")
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

    @Inject
    CustomerRepository customers;

    @Inject
    AccountRepository accounts;

    @Test
    void persist_and_find_roundtrip() {
        String email = "it+" + UUID.randomUUID() + "@example.com";
        Customer c = customers.save(new Customer("IT user", email));
        assertNotNull(c.getId());

        Account a = accounts.save(new Account(
                "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                c, AccountType.CHECKING, new BigDecimal("100.00")));

        assertTrue(accounts.findByNumber(a.getAccountNumber()).isPresent());
        assertTrue(customers.findByEmail(email).isPresent());
    }
}
