package org.ejblab.banking.l10;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferRequest;
import org.ejblab.banking.domain.TransferStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ArquillianExtension.class)
public class Lesson10IT {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "banking-lesson-10-end-to-end.war")
                .addPackages(true, "org.ejblab.banking.l10", "org.ejblab.banking.domain")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("db/migration/V1__baseline.sql", "db/migration/V1__baseline.sql")
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve("org.flywaydb:flyway-core", "org.flywaydb:flyway-database-postgresql")
                        .withTransitivity().asFile());
    }

    @Inject TransferFacade facade;
    @PersistenceContext(unitName = "bankingPU") EntityManager em;

    @Test
    void fullFlowPersistsCompletesAndLedgers() throws Exception {
        TransferRequest req = TransferRequest.of("E2E-001", "E2E-002", new BigDecimal("10.00"));
        Transfer t = facade.submit(req);
        assertNotNull(t.getId());
        assertEquals(TransferStatus.PENDING, t.getStatus());

        // Wait for the MDB + processor to complete asynchronously.
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        TransferStatus status = TransferStatus.PENDING;
        while (System.currentTimeMillis() < deadline && status == TransferStatus.PENDING) {
            Thread.sleep(200);
            em.clear();
            status = em.find(Transfer.class, t.getId()).getStatus();
        }
        assertEquals(TransferStatus.COMPLETED, status);

        Long ledgerCount = em.createQuery(
                "select count(l) from LedgerEntry l where l.transfer.id = :id", Long.class)
                .setParameter("id", t.getId()).getSingleResult();
        assertEquals(2L, ledgerCount, "one DEBIT + one CREDIT");
    }
}
