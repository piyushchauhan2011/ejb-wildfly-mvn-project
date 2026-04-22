package org.ejblab.banking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable double-entry ledger row. Every {@link Transfer} that completes
 * produces exactly two {@code LedgerEntry} rows (one DEBIT, one CREDIT).
 * Used from Lesson 2 (JPA) through Lesson 12 for reconciliation.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry implements Serializable {

    public enum Direction { DEBIT, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt = Instant.now();

    protected LedgerEntry() {}

    public LedgerEntry(Account account, Transfer transfer, Direction direction, BigDecimal amount) {
        this.account = account;
        this.transfer = transfer;
        this.direction = direction;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public Transfer getTransfer() { return transfer; }
    public Direction getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPostedAt() { return postedAt; }
}
