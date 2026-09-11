package com.reloop.listings;

import com.reloop.common.exception.BusinessException;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.dto.CreateListingRequest;
import com.reloop.listings.dto.ListingDto;
import com.reloop.listings.repository.ListingRepository;
import com.reloop.listings.service.ListingService;
import com.reloop.support.TestFields;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ProductUnitRepository productUnitRepository;

    private ListingService listingService;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(listingRepository, productUnitRepository,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private ProductUnit unit(Long ownerId, ProductUnit.UnitStatus status) {
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN-" + UUID.randomUUID(), ownerId, status, "A+");
        TestFields.set(unit, "id", UUID.randomUUID());
        return unit;
    }

    private CreateListingRequest request(UUID unitId) {
        return new CreateListingRequest(unitId, "iPhone 15 Pro Grade A+", "Mulus, fullset",
                new BigDecimal("9500000.00"), null);
    }

    @Test
    @DisplayName("Seller can list an AVAILABLE unit it owns; unit flips to LISTED")
    void testCreateListingSuccess() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.AVAILABLE);
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));
        when(listingRepository.findActiveByUnitId(unit.getId())).thenReturn(Optional.empty());
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productUnitRepository.save(any(ProductUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingDto dto = listingService.createListing(100L, request(unit.getId()));

        assertThat(dto.status()).isEqualTo("ACTIVE");
        assertThat(dto.sellerId()).isEqualTo(100L);
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.LISTED);
    }

    @Test
    @DisplayName("Create rejected when the unit is owned by someone else")
    void testCreateListingNotOwned() {
        ProductUnit unit = unit(200L, ProductUnit.UnitStatus.AVAILABLE);
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> listingService.createListing(100L, request(unit.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not own this product unit");
        verifyNoInteractions(listingRepository);
    }

    @Test
    @DisplayName("Create rejected when the unit is not listable (e.g. SOLD)")
    void testCreateListingUnitNotListable() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.SOLD);
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> listingService.createListing(100L, request(unit.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not listable");
    }

    @Test
    @DisplayName("Create rejected when the unit already has an active listing")
    void testCreateListingAlreadyActive() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.READY_FOR_LISTING);
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));
        when(listingRepository.findActiveByUnitId(unit.getId()))
                .thenReturn(Optional.of(new Listing(unit.getId(), 100L, "old", null,
                        new BigDecimal("9000000.00"), "A+")));

        assertThatThrownBy(() -> listingService.createListing(100L, request(unit.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an active listing");
    }

    @Test
    @DisplayName("Pause flips ACTIVE -> PAUSED and the unit back to READY_FOR_LISTING")
    void testPauseSuccess() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.LISTED);
        Listing listing = new Listing(unit.getId(), 100L, "iPhone", null, new BigDecimal("9500000.00"), "A+");
        TestFields.set(listing, "id", UUID.randomUUID());
        when(listingRepository.findByIdOptional(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));

        ListingDto dto = listingService.pauseListing(listing.getId(), 100L);

        assertThat(dto.status()).isEqualTo("PAUSED");
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.READY_FOR_LISTING);
    }

    @Test
    @DisplayName("Pause rejected for a non-owner")
    void testPauseNotOwned() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.LISTED);
        Listing listing = new Listing(unit.getId(), 100L, "iPhone", null, new BigDecimal("9500000.00"), "A+");
        TestFields.set(listing, "id", UUID.randomUUID());
        when(listingRepository.findByIdOptional(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.pauseListing(listing.getId(), 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to this seller");
    }

    @Test
    @DisplayName("Resume flips PAUSED -> ACTIVE when no other active listing exists")
    void testResumeSuccess() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.READY_FOR_LISTING);
        Listing listing = new Listing(unit.getId(), 100L, "iPhone", null, new BigDecimal("9500000.00"), "A+");
        listing.setStatus(Listing.ListingStatus.PAUSED);
        TestFields.set(listing, "id", UUID.randomUUID());
        when(listingRepository.findByIdOptional(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.findActiveByUnitId(unit.getId())).thenReturn(Optional.empty());
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));

        ListingDto dto = listingService.resumeListing(listing.getId(), 100L);

        assertThat(dto.status()).isEqualTo("ACTIVE");
        assertThat(unit.getStatus()).isEqualTo(ProductUnit.UnitStatus.LISTED);
    }

    @Test
    @DisplayName("Resume rejected when another active listing already covers the unit")
    void testResumeConflict() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.READY_FOR_LISTING);
        Listing listing = new Listing(unit.getId(), 100L, "iPhone", null, new BigDecimal("9500000.00"), "A+");
        listing.setStatus(Listing.ListingStatus.PAUSED);
        TestFields.set(listing, "id", UUID.randomUUID());
        when(listingRepository.findByIdOptional(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.findActiveByUnitId(unit.getId()))
                .thenReturn(Optional.of(new Listing(unit.getId(), 100L, "other", null,
                        new BigDecimal("9000000.00"), "A+")));

        assertThatThrownBy(() -> listingService.resumeListing(listing.getId(), 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an active listing");
        verify(listingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Create with a non-array images payload is rejected")
    void testCreateListingInvalidImages() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.AVAILABLE);
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));
        when(listingRepository.findActiveByUnitId(unit.getId())).thenReturn(Optional.empty());

        CreateListingRequest badImages = new CreateListingRequest(unit.getId(), "iPhone", null,
                new BigDecimal("9500000.00"), "{\"not\":\"an array\"}");

        assertThatThrownBy(() -> listingService.createListing(100L, badImages))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JSON array");
        verify(listingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("PATCH updates only the provided fields")
    void testUpdateListingPartial() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.LISTED);
        Listing listing = new Listing(unit.getId(), 100L, "Old title", "Old desc",
                new BigDecimal("9500000.00"), "A+");
        TestFields.set(listing, "id", UUID.randomUUID());
        when(listingRepository.findByIdOptional(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingDto dto = listingService.updateListing(listing.getId(), 100L,
                new com.reloop.listings.dto.UpdateListingRequest("New title", null, null, "[\"https://img/1.jpg\"]"));

        assertThat(dto.title()).isEqualTo("New title");
        assertThat(dto.description()).isEqualTo("Old desc");
        assertThat(dto.askingPrice()).isEqualByComparingTo(new BigDecimal("9500000.00"));
        assertThat(dto.images()).isEqualTo("[\"https://img/1.jpg\"]");
    }

    @Test
    @DisplayName("PATCH with a non-array images payload is rejected")
    void testUpdateListingInvalidImages() {
        ProductUnit unit = unit(100L, ProductUnit.UnitStatus.LISTED);
        Listing listing = new Listing(unit.getId(), 100L, "Old title", null, new BigDecimal("9500000.00"), "A+");
        TestFields.set(listing, "id", UUID.randomUUID());
        when(listingRepository.findByIdOptional(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.updateListing(listing.getId(), 100L,
                new com.reloop.listings.dto.UpdateListingRequest(null, null, null, "not-json")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JSON array");
    }
}
