package org.ejblab.banking.l04;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Same functional role as {@link ContainerLockedCache}, but the bean tells
 * the container "I handle my own locking, get out of my way".
 *
 * <p>Why would you choose this? Two reasons:
 * <ol>
 *   <li>More fine-grained locking than method-level (e.g. per-key locks).</li>
 *   <li>Explicit upgrade / downgrade patterns a {@code @Lock(WRITE)} can't
 *       express.</li>
 * </ol>
 *
 * <p>Trade-off: YOU are responsible for correctness. No accidental
 * synchronization: every field access is a potential race.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class BeanManagedCache {

    private final Map<String, Long> counters = new HashMap<>();
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    @PostConstruct
    void seed() {
        counters.put("transfers", 0L);
        counters.put("failures",  0L);
    }

    public Optional<Long> read(String key) {
        rwl.readLock().lock();
        try { return Optional.ofNullable(counters.get(key)); }
        finally { rwl.readLock().unlock(); }
    }

    public long increment(String key) {
        rwl.writeLock().lock();
        try {
            long v = counters.getOrDefault(key, 0L) + 1;
            counters.put(key, v);
            return v;
        } finally { rwl.writeLock().unlock(); }
    }
}
