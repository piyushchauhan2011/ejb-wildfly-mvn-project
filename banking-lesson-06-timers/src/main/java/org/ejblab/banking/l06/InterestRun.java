package org.ejblab.banking.l06;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Idempotency guard for the nightly interest accrual job.
 *
 * <p>The table has {@code run_date} as PRIMARY KEY. A second timer fire
 * for the same date tries to INSERT and fails with a unique-constraint
 * violation - which we use as our "already ran, skip" signal.
 */
@Entity
@Table(name = "interest_runs")
public class InterestRun implements Serializable {

    @Id
    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "accounts_processed", nullable = false)
    private int accountsProcessed = 0;

    @Column(name = "total_credited", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredited = BigDecimal.ZERO;

    public InterestRun() {}
    public InterestRun(LocalDate runDate) { this.runDate = runDate; }

    public LocalDate getRunDate() { return runDate; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getAccountsProcessed() { return accountsProcessed; }
    public BigDecimal getTotalCredited() { return totalCredited; }

    public void complete(int processed, BigDecimal total) {
        this.accountsProcessed = processed;
        this.totalCredited = total;
        this.finishedAt = Instant.now();
    }
}
