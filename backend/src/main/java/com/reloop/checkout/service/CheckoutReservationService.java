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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CheckoutReservationService {
    private static final int LEASE_MINUTES = 15;

    private final ProductUnitRepository productUnitRepository;
    private final ListingRepository listingRepository;
    private final UnitReservationRepository reservationRepository;

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
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Step 2: Listing must be active and belong to this unit.
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!listing.getUnitId().equals(unit.getId()) || listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new BusinessException("Listing is not active for this product unit", "LISTING_NOT_ACTIVE", HttpStatus.CONFLICT);
        }

        // Step 3: Strict Availability Invariant Check
        if (unit.getStatus() != ProductUnit.UnitStatus.AVAILABLE && unit.getStatus() != ProductUnit.UnitStatus.LISTED) {
            throw new BusinessException("Product unit is not available for reservation", "UNIT_NOT_AVAILABLE", HttpStatus.CONFLICT);
        }

        // Step 4: Check for existing active reservation
        reservationRepository.findByUnitIdAndStatus(unit.getId(), UnitReservation.ReservationStatus.ACTIVE)
                .ifPresent(r -> {
                    if (r.getExpiresAt().isAfter(Instant.now())) {
                        throw new BusinessException("Unit is already reserved by another customer", "UNIT_ALREADY_RESERVED", HttpStatus.CONFLICT);
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
