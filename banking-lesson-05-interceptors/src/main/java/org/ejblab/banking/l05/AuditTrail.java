package org.ejblab.banking.l05;

import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory audit sink, deliberately simple so the lesson stays focused on
 * the interceptor story. Lesson 10 persists audits through an MDB pipeline.
 */
@Singleton
public class AuditTrail {

    public record Entry(Instant at, String target, String args,
                        String outcome, long durationMicros, String error) {}

    private final List<Entry> entries = new ArrayList<>();

    @Lock(LockType.WRITE)
    public void record(Entry e) { entries.add(e); }

    @Lock(LockType.READ)
    public List<Entry> all() { return Collections.unmodifiableList(new ArrayList<>(entries)); }

    @Lock(LockType.WRITE)
    public void clear() { entries.clear(); }
}
