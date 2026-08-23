package com.reloop.listings.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listings")
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal askingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(nullable = false, length = 5)
    private String gradeSnapshot;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum ListingStatus {
        DRAFT, PENDING_REVIEW, ACTIVE, PAUSED, SOLD, REMOVED
    }

    public Listing() {}

    public Listing(UUID unitId, Long sellerId, String title, String description, BigDecimal askingPrice, String gradeSnapshot) {
        this.unitId = unitId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.askingPrice = askingPrice;
        this.gradeSnapshot = gradeSnapshot;
        this.status = ListingStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public Long getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAskingPrice() { return askingPrice; }
    public void setAskingPrice(BigDecimal askingPrice) { this.askingPrice = askingPrice; }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }
    public String getGradeSnapshot() { return gradeSnapshot; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
