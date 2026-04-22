package org.ejblab.banking.l07;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferRequest;
import org.ejblab.banking.domain.TransferStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class Lesson07IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "lesson07-it.war")
                .addPackages(true, "org.ejblab.banking.domain")
                .addPackages(true, "org.ejblab.banking.l07")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsWebInfResource("beans.xml", "beans.xml")
                .addAsLibraries(Maven.resolver()
                        .resolve("org.flywaydb:flyway-core:10.20.1",
                                 "org.flywaydb:flyway-database-postgresql:10.20.1")
                        .withTransitivity().asFile());
    }

    @PersistenceContext(unitName = "bankingPU") EntityManager em;
    @Inject SeedBean seed;
    @Inject TransferService sync;

    @Test
    void sync_transfer_is_idempotent_by_client_request_id() {
        seed.seedIfMissing();
        TransferRequest req = TransferRequest.of("L7-001", "L7-002", new BigDecimal("1.00"));
        Transfer first = sync.transfer(req);
        Transfer second = sync.transfer(req);  // same clientRequestId -> return existing
        assertEquals(first.getId(), second.getId());
        assertEquals(TransferStatus.COMPLETED, first.getStatus());
    }
}
