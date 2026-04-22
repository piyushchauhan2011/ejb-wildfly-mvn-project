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
import jakarta.persistence.Version;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A transfer intent between two accounts. Moves through {@link TransferStatus}
 * lifecycle: PENDING -> COMPLETED or FAILED.
 *
 * <p>The {@code clientRequestId} column is used for idempotency: resubmitting
 * the same {@code clientRequestId} must NOT produce a duplicate transfer
 * (see Lesson 7's MDB and Lesson 11's retry interceptor).
 */
@Entity
@Table(name = "transfers")
public class Transfer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_request_id", nullable = false, unique = true, length = 64)
    private String clientRequestId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;

    public Transfer() {}

    public Transfer(Account from, Account to, BigDecimal amount) {
        this.fromAccount = from;
        this.toAccount = to;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String id) { this.clientRequestId = id; }
    public Account getFromAccount() { return fromAccount; }
    public void setFromAccount(Account from) { this.fromAccount = from; }
    public Account getToAccount() { return toAccount; }
    public void setToAccount(Account to) { this.toAccount = to; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransferStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getVersion() { return version; }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }
}
