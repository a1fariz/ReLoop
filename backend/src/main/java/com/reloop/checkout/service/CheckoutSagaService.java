package com.reloop.checkout.service;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.repository.UnitReservationRepository;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.repository.ListingRepository;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutSagaService {
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.15"); // 15% platform commission

    private final UnitReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final ProductUnitRepository productUnitRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final DoubleEntryLedgerService ledgerService;

    public CheckoutSagaService(
            UnitReservationRepository reservationRepository,
            ListingRepository listingRepository,
            ProductUnitRepository productUnitRepository,
            MasterOrderRepository masterOrderRepository,
            FulfillmentOrderRepository fulfillmentOrderRepository,
            DoubleEntryLedgerService ledgerService
    ) {
        this.reservationRepository = reservationRepository;
        this.listingRepository = listingRepository;
        this.productUnitRepository = productUnitRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public OrderConfirmationResponse processPaymentAndSettleOrder(Long buyerId, ConfirmPaymentRequest request) {
        // Step 1: Validate active 15-min reservation lease
        UnitReservation reservation = reservationRepository.findByToken(request.reservationToken())
                .orElseThrow(() -> new BusinessException("Invalid reservation token", "RESERVATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (reservation.getStatus() != UnitReservation.ReservationStatus.ACTIVE || reservation.getExpiresAt().isBefore(Instant.now())) {
            reservation.setStatus(UnitReservation.ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            throw new BusinessException("Reservation lease has expired. Unit returned to marketplace.", "LEASE_EXPIRED", HttpStatus.GONE);
        }

        // Step 2: Acquire row lock on unit & listing
        ProductUnit unit = productUnitRepository.findByIdForUpdate(reservation.getUnitId())
                .orElseThrow(() -> new BusinessException("Unit not found", "UNIT_NOT_FOUND", HttpStatus.NOT_FOUND));

        Listing listing = listingRepository.findById(reservation.getListingId())
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Step 3: Compute exact server-authoritative financial breakdown
        BigDecimal totalAmount = listing.getAskingPrice();
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerNet = totalAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        // Step 4: Create Master Order & Fulfillment Sub-Order
        String orderNumber = "ORD-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MasterOrder masterOrder = new MasterOrder(orderNumber, buyerId, totalAmount, request.shippingAddress());
        masterOrder.setPaymentStatus(MasterOrder.PaymentStatus.PAID);
        masterOrder = masterOrderRepository.save(masterOrder);

        FulfillmentOrder fulfillment = new FulfillmentOrder(
                masterOrder.getId(),
                listing.getSellerId(),
                unit.getId(),
                totalAmount,
                platformFee,
                sellerNet
        );
        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.HELD);
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.PROCESSING);
        fulfillment = fulfillmentOrderRepository.save(fulfillment);

        // Step 5: Post Double-Entry Financial Journal (Lock Buyer Funds into Escrow)
        // DR: GATEWAY_CLEARING, CR: ESCROW_HELD (Balanced)
        ledgerService.postJournal(
                "ORDER_PAYMENT",
                masterOrder.getOrderNumber(),
                "Escrow lock for order " + masterOrder.getOrderNumber(),
                List.of(
                        new DoubleEntryLedgerService.PostingLine("GATEWAY_CLEARING", FinancialLedgerLine.EntryType.DR, totalAmount),
                        new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.CR, totalAmount)
                )
        );

        // Step 6: Convert reservation & Mark unit & listing as SOLD
        reservation.setStatus(UnitReservation.ReservationStatus.CONVERTED);
        reservationRepository.save(reservation);

        unit.setStatus(ProductUnit.UnitStatus.SOLD);
        unit.setCurrentCustody(ProductUnit.PhysicalCustody.LOGISTICS_3PL);
        productUnitRepository.save(unit);

        listing.setStatus(Listing.ListingStatus.SOLD);
        listingRepository.save(listing);

        return new OrderConfirmationResponse(
                masterOrder.getId(),
                masterOrder.getOrderNumber(),
                fulfillment.getId(),
                unit.getId(),
                totalAmount,
                platformFee,
                sellerNet,
                masterOrder.getPaymentStatus().name(),
                fulfillment.getEscrowStatus().name(),
                masterOrder.getCreatedAt()
        );
    }
}
