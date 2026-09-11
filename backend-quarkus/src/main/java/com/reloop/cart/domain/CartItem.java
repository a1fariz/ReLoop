package com.reloop.cart.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only cart snapshot: rows are immutable (no @Version); quantity is
 * adjusted by rewriting the row. Adding to a cart never locks inventory —
 * anti-hoarding reservations happen only at checkout initiation.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    public CartItem() {}

    public CartItem(Long userId, UUID listingId, int quantity) {
        this.userId = userId;
        this.listingId = listingId;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public UUID getListingId() { return listingId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Instant getAddedAt() { return addedAt; }
}
