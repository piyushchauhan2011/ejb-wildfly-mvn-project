package org.ejblab.banking.l10;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Transfer;
import org.ejblab.banking.domain.TransferStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

/**
 * Stage 4: Timer reconciling stuck PENDING transfers. Anything older than
 * five minutes without a terminal status is suspect; we log for now and
 * would re-publish in production (idempotency makes that safe).
 */
@Singleton
public class ReconcileBean {

    private static final Logger log = Logger.getLogger("RECONCILE");

    @PersistenceContext(unitName = "bankingPU")
    EntityManager em;

    @Schedule(second = "0", minute = "*/1", hour = "*", persistent = false, info = "reconcile-pending")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void reconcile() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<Transfer> stuck = em.createQuery(
                "select t from Transfer t where t.status = :s and t.createdAt < :c", Transfer.class)
                .setParameter("s", TransferStatus.PENDING)
                .setParameter("c", cutoff)
                .getResultList();

        for (Transfer t : stuck) {
            log.warning("STUCK transfer " + t.getClientRequestId() + " age="
                    + ChronoUnit.SECONDS.between(t.getCreatedAt(), Instant.now()) + "s");
            // Production: republish to TransfersRequested; the processor is idempotent.
        }
    }
}
