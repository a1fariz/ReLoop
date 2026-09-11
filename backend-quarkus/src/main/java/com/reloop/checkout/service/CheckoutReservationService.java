package com.reloop.checkout.service;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.dto.ReservationResponse;
import com.reloop.checkout.dto.ReserveUnitRequest;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.common.exception.BusinessException;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.repository.ListingRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class CheckoutReservationService {
    private static final int LEASE_MINUTES = 15;

    private final ProductUnitRepository productUnitRepository;
    private final ListingRepository listingRepository;
    private final UnitReservationRepository reservationRepository;

    @Inject
    public CheckoutReservationService(
            ProductUnitRepository productUnitRepository,
            ListingRepository listingRepository,
            UnitReservationRepository reservationRepository
    ) {
        this.productUnitRepository = productUnitRepository;
        this.listingRepository = listingRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationResponse createReservationLease(Long userId, ReserveUnitRequest request) {
        // Step 1: Pessimistic lock row on ProductUnit
        ProductUnit unit = productUnitRepository.findByIdForUpdate(request.getUnitId())
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        // Step 2: Listing must be active and belong to this unit.
        Listing listing = listingRepository.findByIdOptional(request.getListingId())
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", 404));
        if (!listing.getUnitId().equals(unit.getId()) || listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new BusinessException("Listing is not active for this product unit", "LISTING_NOT_ACTIVE", 409);
        }

        // Step 3: Strict Availability Invariant Check
        if (unit.getStatus() != ProductUnit.UnitStatus.AVAILABLE && unit.getStatus() != ProductUnit.UnitStatus.LISTED) {
            throw new BusinessException("Product unit is not available for reservation", "UNIT_NOT_AVAILABLE", 409);
        }

        // Step 4: Check for existing active reservation
        reservationRepository.findByUnitIdAndStatus(unit.getId(), UnitReservation.ReservationStatus.ACTIVE)
                .ifPresent(r -> {
                    if (r.getExpiresAt().isAfter(Instant.now())) {
                        throw new BusinessException("Unit is already reserved by another customer", "UNIT_ALREADY_RESERVED", 409);
                    } else {
                        r.setStatus(UnitReservation.ReservationStatus.EXPIRED);
                        reservationRepository.save(r);
                    }
                });

        // Step 4: Create new 15-minute lease
        Instant expiresAt = Instant.now().plus(LEASE_MINUTES, ChronoUnit.MINUTES);
        UnitReservation reservation = new UnitReservation(unit.getId(), userId, request.getListingId(), expiresAt);
        reservation = reservationRepository.save(reservation);

        // Step 5: Mutate unit state to RESERVED
        unit.setStatus(ProductUnit.UnitStatus.RESERVED);
        productUnitRepository.save(unit);

        long remainingSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getUnitId(),
                reservation.getListingId(),
                reservation.getToken(),
                reservation.getExpiresAt(),
                Math.max(0, remainingSeconds)
        );
    }
}
