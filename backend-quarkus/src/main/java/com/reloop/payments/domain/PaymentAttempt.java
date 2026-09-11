package com.reloop.payments.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID masterOrderId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "IDR";

    @Column(nullable = false, length = 30)
    private String gateway = "MOCK_GATEWAY";

    @Column(nullable = false, unique = true, length = 100)
    private String gatewayReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.INITIATED;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false, unique = true, length = 150)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant initiatedAt = Instant.now();

    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Status {
        INITIATED, PROCESSING, SUCCEEDED, FAILED, EXPIRED
    }

    public PaymentAttempt() {}

    public PaymentAttempt(UUID masterOrderId, Long buyerId, BigDecimal amount,
                          String gatewayReference, String idempotencyKey) {
        this.masterOrderId = masterOrderId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.gatewayReference = gatewayReference;
        this.idempotencyKey = idempotencyKey;
        this.status = Status.INITIATED;
        this.initiatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getMasterOrderId() { return masterOrderId; }
    public Long getBuyerId() { return buyerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getGateway() { return gateway; }
    public String getGatewayReference() { return gatewayReference; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
