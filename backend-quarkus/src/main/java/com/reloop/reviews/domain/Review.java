package com.reloop.reviews.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID fulfillmentOrderId;

    @Column(nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Review() {}

    public Review(UUID fulfillmentOrderId, UUID unitId, Long sellerId, Long buyerId, int rating, String comment) {
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.unitId = unitId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.rating = rating;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public UUID getFulfillmentOrderId() { return fulfillmentOrderId; }
    public UUID getUnitId() { return unitId; }
    public Long getSellerId() { return sellerId; }
    public Long getBuyerId() { return buyerId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
