package com.reloop.cart.service;

import com.reloop.cart.domain.CartItem;
import com.reloop.cart.dto.CartDtos;
import com.reloop.cart.repository.CartItemRepository;
import com.reloop.common.exception.BusinessException;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.repository.ListingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read-only snapshot cart. Adding an item never touches inventory — the
 * anti-hoarding reservation lease is only ever acquired at checkout
 * initiation (CheckoutReservationService). Prices are snapshotted at read
 * time from the listing, so a seller price change is always reflected.
 */
@ApplicationScoped
public class CartService {
    private static final int MAX_DISTINCT_LISTINGS = 5;
    private static final int MAX_QUANTITY_PER_LISTING = 5;

    private final CartItemRepository cartItemRepository;
    private final ListingRepository listingRepository;

    @Inject
    public CartService(CartItemRepository cartItemRepository, ListingRepository listingRepository) {
        this.cartItemRepository = cartItemRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional
    public CartDtos.CartItemDto addItem(Long userId, UUID listingId, int quantity) {
        Listing listing = listingRepository.findByIdOptional(listingId)
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", 404));
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new BusinessException("Listing is not active", "LISTING_NOT_ACTIVE", 409);
        }

        CartItem existing = cartItemRepository.findByUserIdAndListingId(userId, listingId).orElse(null);
        if (existing == null) {
            if (cartItemRepository.countByUserId(userId) >= MAX_DISTINCT_LISTINGS) {
                throw new BusinessException("Cart is limited to 5 distinct listings", "CART_LIMIT_REACHED", 409);
            }
            CartItem item = cartItemRepository.save(new CartItem(userId, listingId, clamp(quantity)));
            return toDto(item, listing);
        }

        int newQuantity = existing.getQuantity() + Math.max(quantity, 1);
        if (newQuantity > MAX_QUANTITY_PER_LISTING) {
            throw new BusinessException("Quantity per listing is limited to 5", "CART_QUANTITY_LIMIT", 409);
        }
        existing.setQuantity(newQuantity);
        cartItemRepository.save(existing);
        return toDto(existing, listing);
    }

    @Transactional
    public void removeItem(Long userId, UUID listingId) {
        if (cartItemRepository.deleteByUserIdAndListingId(userId, listingId) == 0) {
            throw new BusinessException("Listing not found in cart", "CART_ITEM_NOT_FOUND", 404);
        }
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public CartDtos.CartDto getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartDtos.CartItemDto> dtos = items.stream()
                .map(item -> toDto(item, listingRepository.findByIdOptional(item.getListingId()).orElse(null)))
                .toList();
        int totalItems = dtos.stream().mapToInt(CartDtos.CartItemDto::quantity).sum();
        return new CartDtos.CartDto(dtos, totalItems, items.size());
    }

    private static int clamp(int quantity) {
        if (quantity < 1) return 1;
        if (quantity > MAX_QUANTITY_PER_LISTING) {
            throw new BusinessException("Quantity per listing is limited to 5", "CART_QUANTITY_LIMIT", 409);
        }
        return quantity;
    }

    private static CartDtos.CartItemDto toDto(CartItem item, Listing listing) {
        return new CartDtos.CartItemDto(
                item.getId(),
                item.getListingId(),
                listing != null ? listing.getUnitId() : null,
                listing != null ? listing.getAskingPrice() : null,
                item.getQuantity(),
                listing != null ? listing.getTitle() : null,
                listing != null ? listing.getStatus().name() : null
        );
    }
}
