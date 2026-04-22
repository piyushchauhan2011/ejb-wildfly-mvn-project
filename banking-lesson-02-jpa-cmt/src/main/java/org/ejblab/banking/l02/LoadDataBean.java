package org.ejblab.banking.l02;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.ejblab.banking.domain.Account;
import org.ejblab.banking.domain.AccountType;
import org.ejblab.banking.domain.Customer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Small micro-benchmark harness exposed over a servlet in Lesson 2.
 *
 * <p>We batch-persist {@code n} accounts inside <em>one</em> JTA transaction
 * ({@code REQUIRES_NEW} guarantees a fresh transaction regardless of
 * whatever the servlet propagated). Hibernate batches up to
 * {@code hibernate.jdbc.batch_size} inserts per round-trip (configured in
 * {@code persistence.xml}).
 *
 * <p>Compare two runs:
 * <ol>
 *   <li>{@link #insertBatch(int)} - one JTA TX, batching enabled</li>
 *   <li>Insert the same rows one-per-call (see README, the
 *       "autocommit vs JTA batch" benchmark).</li>
 * </ol>
 */
@Stateless
public class LoadDataBean {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public long insertBatch(int howMany) {
        long t0 = System.nanoTime();
        Customer c = new Customer("Benchmark User " + UUID.randomUUID(),
                "bench-" + UUID.randomUUID() + "@example.com");
        em.persist(c);

        for (int i = 0; i < howMany; i++) {
            String number = "BM" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            em.persist(new Account(number, c, AccountType.CHECKING, BigDecimal.ZERO));
            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
        return System.nanoTime() - t0;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int insertOnePerTx() {
        Customer c = new Customer("One-per-tx " + UUID.randomUUID(),
                "onetx-" + UUID.randomUUID() + "@example.com");
        em.persist(c);
        return 1;
    }
}
