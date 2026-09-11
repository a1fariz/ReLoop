package com.reloop.cart.repository;

import com.reloop.cart.domain.CartItem;
import com.reloop.common.jpa.ReloopRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CartItemRepository implements ReloopRepository<CartItem, UUID> {

    public List<CartItem> findByUserId(Long userId) {
        return list("userId", Sort.descending("addedAt"), userId);
    }

    public long countByUserId(Long userId) {
        return count("userId", userId);
    }

    public Optional<CartItem> findByUserIdAndListingId(Long userId, UUID listingId) {
        return find("userId = ?1 AND listingId = ?2", userId, listingId).firstResultOptional();
    }

    public long deleteByUserIdAndListingId(Long userId, UUID listingId) {
        return delete("userId = ?1 AND listingId = ?2", userId, listingId);
    }

    public long deleteByUserId(Long userId) {
        return delete("userId", userId);
    }
}
