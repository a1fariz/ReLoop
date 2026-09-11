package com.reloop.cart;

import com.reloop.cart.domain.CartItem;
import com.reloop.cart.repository.CartItemRepository;
import com.reloop.cart.service.CartService;
import com.reloop.common.exception.BusinessException;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ListingRepository listingRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, listingRepository);
    }

    private Listing activeListing(UUID id, String price) {
        Listing listing = new Listing(UUID.randomUUID(), 20L, "iPhone 15 Pro 256GB", "Sealed", new BigDecimal(price), "A");
        com.reloop.support.TestFields.set(listing, "id", id);
        return listing;
    }

    @Test
    @DisplayName("Add a new active listing: item persisted with quantity 1, no inventory touched")
    void testAddNewItem() {
        UUID listingId = UUID.randomUUID();
        Listing listing = activeListing(listingId, "15000000.00");
        when(listingRepository.findByIdOptional(listingId)).thenReturn(Optional.of(listing));
        when(cartItemRepository.findByUserIdAndListingId(10L, listingId)).thenReturn(Optional.empty());
        when(cartItemRepository.countByUserId(10L)).thenReturn(0L);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = cartService.addItem(10L, listingId, 1);

        assertThat(dto.listingId()).isEqualTo(listingId);
        assertThat(dto.quantity()).isEqualTo(1);
        assertThat(dto.price()).isEqualByComparingTo(new BigDecimal("15000000.00"));
        assertThat(dto.unitId()).isEqualTo(listing.getUnitId());
    }

    @Test
    @DisplayName("Adding a listing already in cart increments quantity up to the cap of 5")
    void testIncrementExistingItem() {
        UUID listingId = UUID.randomUUID();
        when(listingRepository.findByIdOptional(listingId)).thenReturn(Optional.of(activeListing(listingId, "100.00")));
        CartItem existing = new CartItem(10L, listingId, 2);
        when(cartItemRepository.findByUserIdAndListingId(10L, listingId)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = cartService.addItem(10L, listingId, 3);

        assertThat(dto.quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Quantity beyond the per-listing cap of 5 is rejected (409)")
    void testQuantityCapRejected() {
        UUID listingId = UUID.randomUUID();
        when(listingRepository.findByIdOptional(listingId)).thenReturn(Optional.of(activeListing(listingId, "100.00")));
        CartItem existing = new CartItem(10L, listingId, 3);
        when(cartItemRepository.findByUserIdAndListingId(10L, listingId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItem(10L, listingId, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limited to 5");
    }

    @Test
    @DisplayName("Sixth distinct listing is rejected: max 5 distinct listings per cart (409)")
    void testDistinctListingLimit() {
        UUID listingId = UUID.randomUUID();
        when(listingRepository.findByIdOptional(listingId)).thenReturn(Optional.of(activeListing(listingId, "100.00")));
        when(cartItemRepository.findByUserIdAndListingId(10L, listingId)).thenReturn(Optional.empty());
        when(cartItemRepository.countByUserId(10L)).thenReturn(5L);

        assertThatThrownBy(() -> cartService.addItem(10L, listingId, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5 distinct listings");
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Non-active listings cannot be added to the cart (409)")
    void testInactiveListingRejected() {
        UUID listingId = UUID.randomUUID();
        Listing listing = activeListing(listingId, "100.00");
        listing.setStatus(Listing.ListingStatus.SOLD);
        when(listingRepository.findByIdOptional(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> cartService.addItem(10L, listingId, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("getCart enriches items with listing snapshots and computes totals")
    void testGetCartTotals() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        CartItem first = new CartItem(10L, firstId, 2);
        com.reloop.support.TestFields.set(first, "id", UUID.randomUUID());
        CartItem second = new CartItem(10L, secondId, 3);
        com.reloop.support.TestFields.set(second, "id", UUID.randomUUID());

        when(cartItemRepository.findByUserId(10L)).thenReturn(java.util.List.of(first, second));
        lenient().when(listingRepository.findByIdOptional(firstId))
                .thenReturn(Optional.of(activeListing(firstId, "15000000.00")));
        lenient().when(listingRepository.findByIdOptional(secondId))
                .thenReturn(Optional.of(activeListing(secondId, "5000000.00")));

        var cart = cartService.getCart(10L);

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.totalItems()).isEqualTo(5);
        assertThat(cart.distinctListings()).isEqualTo(2);
        assertThat(cart.items()).extracting(item -> item.listingId()).containsExactly(firstId, secondId);
    }

    @Test
    @DisplayName("removeItem of a listing not in the cart is a 404")
    void testRemoveMissingItem() {
        UUID listingId = UUID.randomUUID();
        when(cartItemRepository.deleteByUserIdAndListingId(10L, listingId)).thenReturn(0L);

        assertThatThrownBy(() -> cartService.removeItem(10L, listingId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found in cart");
    }
}
