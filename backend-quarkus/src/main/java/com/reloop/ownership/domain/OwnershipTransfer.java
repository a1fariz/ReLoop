package com.reloop.ownership.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ownership_transfers")
public class OwnershipTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID unitId;

    private Long fromOwnerId;

    @Column(nullable = false)
    private Long toOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferType transferType;

    @Column(nullable = false, length = 50)
    private String referenceType;

    private UUID referenceId;

    @Column(nullable = false)
    private Instant transferredAt = Instant.now();

    public enum TransferType {
        PURCHASE, TRADE_IN, RETURN, PLATFORM_RECOVERY
    }

    public OwnershipTransfer() {}

    public OwnershipTransfer(UUID unitId, Long fromOwnerId, Long toOwnerId, TransferType transferType, String referenceType, UUID referenceId) {
        this.unitId = unitId;
        this.fromOwnerId = fromOwnerId;
        this.toOwnerId = toOwnerId;
        this.transferType = transferType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public Long getFromOwnerId() { return fromOwnerId; }
    public Long getToOwnerId() { return toOwnerId; }
    public TransferType getTransferType() { return transferType; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public Instant getTransferredAt() { return transferredAt; }
}
