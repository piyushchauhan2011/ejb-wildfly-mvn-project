package org.ejblab.banking.l03;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates the "retry on serialization failure" pattern that Postgres
 * forces on you when the isolation level is SERIALIZABLE.
 *
 * <p>Postgres implements SSI (Serializable Snapshot Isolation): concurrent
 * read-write transactions that would produce a non-serializable history
 * fail with SQLSTATE {@code 40001} ("could not serialize access due to
 * concurrent update"). The correct application response is to retry the
 * whole transaction (with backoff), up to a limit.
 *
 * <p>Because a retry requires a NEW transaction, each attempt calls a
 * {@link TransactionAttributeType#REQUIRES_NEW} method.
 */
@Stateless
public class SerializableRetryService {

    private static final Logger log = Logger.getLogger(SerializableRetryService.class.getName());
    private static final int MAX_ATTEMPTS = 5;

    @Inject
    TransferService transferService;

    /**
     * Runs the transfer, retrying on SQLSTATE 40001 up to {@value #MAX_ATTEMPTS}
     * times with exponential backoff + jitter.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED) // ensure the attempt starts a fresh TX
    public void transferWithRetry(String from, String to, BigDecimal amount) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                transferService.transfer(from, to, amount);  // REQUIRED -> new TX because caller has none
                return;
            } catch (RuntimeException e) {
                if (!isSerializationFailure(e) || attempt == MAX_ATTEMPTS) throw e;
                long sleepMs = (long) (Math.pow(2, attempt) + ThreadLocalRandom.current().nextLong(10));
                log.log(Level.FINE, "Serialization failure, retry {0}/{1} after {2} ms",
                        new Object[]{attempt, MAX_ATTEMPTS, sleepMs});
                try { Thread.sleep(sleepMs); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    /**
     * Walks the cause chain looking for a {@link SQLException} with SQLSTATE
     * class 40 (transaction rollback). 40001 in particular is
     * "serialization failure" on Postgres, 40P01 is "deadlock detected".
     *
     * <p>We intentionally do NOT import {@code org.postgresql.util.PSQLException}
     * to keep the driver out of the WAR classpath: the driver lives as a
     * WildFly module.
     */
    private static boolean isSerializationFailure(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof SQLException sql) {
                String state = sql.getSQLState();
                if (state != null && (state.equals("40001") || state.equals("40P01"))) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}
