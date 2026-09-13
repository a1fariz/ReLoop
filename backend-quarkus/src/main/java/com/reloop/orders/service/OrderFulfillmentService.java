package com.reloop.orders.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.repository.FinancialJournalEntryRepository;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import com.reloop.warranties.service.WarrantyService;
import com.reloop.notifications.service.NotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fulfillment lifecycle: PROCESSING → SHIPPED → DELIVERED → COMPLETED.
 * Completion settles the escrow (SELLER_PAYABLE + PLATFORM_REVENUE) and issues
 * the standard warranty, all in the same transaction as the state transition.
 */
@ApplicationScoped
public class OrderFulfillmentService {
    private static final Logger log = Logger.getLogger(OrderFulfillmentService.class);

    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final ProductUnitRepository productUnitRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final WarrantyService warrantyService;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final FinancialJournalEntryRepository journalRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;
    private final UUID instanceCorrelationId;

    @Inject
    public OrderFulfillmentService(
            FulfillmentOrderRepository fulfillmentOrderRepository,
            MasterOrderRepository masterOrderRepository,
            ProductUnitRepository productUnitRepository,
            DoubleEntryLedgerService ledgerService,
            WarrantyService warrantyService,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            FinancialJournalEntryRepository journalRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            NotificationService notificationService
    ) {
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.productUnitRepository = productUnitRepository;
        this.ledgerService = ledgerService;
        this.warrantyService = warrantyService;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.journalRepository = journalRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.notificationService = notificationService;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<MasterOrder> getBuyerOrders(Long buyerId, int page, int size) {
        var query = masterOrderRepository.find("buyerId = ?1",
                io.quarkus.panache.common.Sort.descending("createdAt"), buyerId);
        long total = query.count();
        List<MasterOrder> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items, total, page, size);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<FulfillmentOrder> getSellerFulfillments(Long sellerUserId, int page, int size) {
        Long sellerProfileId = fulfillmentOrderRepository.resolveSellerProfileId(sellerUserId);
        var query = fulfillmentOrderRepository.find("sellerId = ?1",
                io.quarkus.panache.common.Sort.descending("createdAt"), sellerProfileId);
        long total = query.count();
        List<FulfillmentOrder> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items, total, page, size);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<FulfillmentOrder> getAllFulfillments(int page, int size) {
        var query = fulfillmentOrderRepository.findAll(
                io.quarkus.panache.common.Sort.descending("createdAt"));
        long total = query.count();
        List<FulfillmentOrder> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items, total, page, size);
    }

    @Transactional
    public FulfillmentOrder ship(UUID fulfillmentId, Long sellerUserId, String trackingNumber, String courierName) {
        FulfillmentOrder fulfillment = loadOwned(fulfillmentId, sellerUserId);
        requireStatus(fulfillment, FulfillmentOrder.FulfillmentStatus.PROCESSING, "ship");

        fulfillment.setTrackingNumber(trackingNumber);
        fulfillment.setCourierName(courierName);
        fulfillment.setShippedAt(Instant.now());
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.SHIPPED);
        fulfillment = fulfillmentOrderRepository.save(fulfillment);

        auditService.record("FulfillmentOrder", fulfillment.getId().toString(), "STATE_TRANSITION", sellerUserId,
                null, FulfillmentOrder.FulfillmentStatus.PROCESSING.name(), FulfillmentOrder.FulfillmentStatus.SHIPPED.name());
        emit("FULFILLMENT_SHIPPED", fulfillment);
        notifyBuyer(fulfillment, "Order shipped", "Your order is on the way. Tracking: " + courierName + " · " + trackingNumber, "ORDER");
        return fulfillment;
    }

    @Transactional
    public FulfillmentOrder deliver(UUID fulfillmentId) {
        FulfillmentOrder fulfillment = load(fulfillmentId);
        requireStatus(fulfillment, FulfillmentOrder.FulfillmentStatus.SHIPPED, "deliver");

        fulfillment.setDeliveredAt(Instant.now());
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.DELIVERED);
        fulfillment = fulfillmentOrderRepository.save(fulfillment);

        auditService.record("FulfillmentOrder", fulfillment.getId().toString(), "STATE_TRANSITION", null,
                null, FulfillmentOrder.FulfillmentStatus.SHIPPED.name(), FulfillmentOrder.FulfillmentStatus.DELIVERED.name());
        emit("FULFILLMENT_DELIVERED", fulfillment);
        notifyBuyer(fulfillment, "Order delivered", "Your order has been delivered and is ready for review.", "ORDER");
        return fulfillment;
    }

    @Transactional
    public FulfillmentOrder complete(UUID fulfillmentId) {
        FulfillmentOrder fulfillment = load(fulfillmentId);
        requireStatus(fulfillment, FulfillmentOrder.FulfillmentStatus.DELIVERED, "complete");

        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));

        // Escrow settlement: DR ESCROW_HELD total / CR SELLER_PAYABLE seller net / CR PLATFORM_REVENUE fee
        ledgerService.postJournal("ESCROW_SETTLEMENT", masterOrder.getOrderNumber(),
                "Escrow release for order " + masterOrder.getOrderNumber(),
                List.of(
                        new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.DR, fulfillment.getSubtotalAmount()),
                        new DoubleEntryLedgerService.PostingLine("SELLER_PAYABLE:" + fulfillment.getSellerId(),
                                FinancialLedgerLine.EntryType.CR, fulfillment.getSellerNetAmount()),
                        new DoubleEntryLedgerService.PostingLine("PLATFORM_REVENUE",
                                FinancialLedgerLine.EntryType.CR, fulfillment.getPlatformFeeAmount())
                ));

        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.SETTLED);
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.COMPLETED);
        fulfillment = fulfillmentOrderRepository.save(fulfillment);

        // Legal ownership + custody transfer: the buyer becomes the unit's owner
        ProductUnit unit = productUnitRepository.findByIdOptional(fulfillment.getUnitId())
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));
        unit.setCurrentOwnerId(masterOrder.getBuyerId());
        unit.setCurrentCustody(ProductUnit.PhysicalCustody.BUYER);
        unit.setStatus(ProductUnit.UnitStatus.OWNED);
        productUnitRepository.save(unit);

        warrantyService.issueStandardWarranty(fulfillment.getUnitId(), masterOrder.getBuyerId(), fulfillment.getId());

        auditService.record("FulfillmentOrder", fulfillment.getId().toString(), "STATE_TRANSITION", null,
                null, FulfillmentOrder.FulfillmentStatus.DELIVERED.name(), FulfillmentOrder.FulfillmentStatus.COMPLETED.name());
        emit("FULFILLMENT_COMPLETED", fulfillment);
        meterRegistry.counter("reloop.fulfillment.completed").increment();
        log.infof("Fulfillment %s completed; escrow settled, warranty issued for unit %s",
                fulfillment.getId(), fulfillment.getUnitId());
        return fulfillment;
    }

    /**
     * Disburses the seller payout from SELLER_PAYABLE to DISBURSEMENT_BANK.
     * Idempotent via the ledger: a SELLER_PAYOUT journal for this fulfillment
     * makes any second attempt a 409.
     */
    @Transactional
    public FulfillmentOrder disbursePayout(UUID fulfillmentId) {
        FulfillmentOrder fulfillment = load(fulfillmentId);
        if (fulfillment.getFulfillmentStatus() != FulfillmentOrder.FulfillmentStatus.COMPLETED) {
            throw new BusinessException("Payout requires a COMPLETED fulfillment", "INVALID_FULFILLMENT_STATE", 409);
        }
        if (journalRepository.findByReferenceTypeAndReferenceId("SELLER_PAYOUT", fulfillment.getId().toString()).isPresent()) {
            throw new BusinessException("Payout already disbursed for this fulfillment", "ALREADY_DISBURSED", 409);
        }

        ledgerService.postJournal("SELLER_PAYOUT", fulfillment.getId().toString(),
                "Seller payout for fulfillment " + fulfillment.getId(),
                List.of(
                        new DoubleEntryLedgerService.PostingLine("SELLER_PAYABLE:" + fulfillment.getSellerId(),
                                FinancialLedgerLine.EntryType.DR, fulfillment.getSellerNetAmount()),
                        new DoubleEntryLedgerService.PostingLine("DISBURSEMENT_BANK",
                                FinancialLedgerLine.EntryType.CR, fulfillment.getSellerNetAmount())
                ));

        auditService.record("FulfillmentOrder", fulfillment.getId().toString(), "PAYOUT", null,
                null, "SELLER_PAYABLE:" + fulfillment.getSellerId(), "DISBURSEMENT_BANK");
        emit("SELLER_PAYOUT", fulfillment);
        meterRegistry.counter("reloop.fulfillment.payout").increment();
        log.infof("Payout disbursed for fulfillment %s: %s", fulfillment.getId(), fulfillment.getSellerNetAmount());
        return fulfillment;
    }

    private void notifyBuyer(FulfillmentOrder fulfillment, String title, String body, String category) {
        MasterOrder order = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        notificationService.push(order.getBuyerId(), com.reloop.notifications.domain.Notification.Category.valueOf(category),
                title, body, "FULFILLMENT_ORDER", fulfillment.getId());
    }

    private FulfillmentOrder load(UUID fulfillmentId) {
        return fulfillmentOrderRepository.findByIdOptional(fulfillmentId)
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));
    }

    private FulfillmentOrder loadOwned(UUID fulfillmentId, Long sellerUserId) {
        FulfillmentOrder fulfillment = load(fulfillmentId);
        Long ownerUserId = fulfillmentOrderRepository.resolveSellerUserId(fulfillment.getSellerId());
        if (ownerUserId == null || !ownerUserId.equals(sellerUserId)) {
            throw new BusinessException("Fulfillment order does not belong to this seller", "FULFILLMENT_FORBIDDEN", 403);
        }
        return fulfillment;
    }

    private void requireStatus(FulfillmentOrder fulfillment, FulfillmentOrder.FulfillmentStatus expected, String action) {
        if (fulfillment.getFulfillmentStatus() != expected) {
            throw new BusinessException(
                    String.format("Fulfillment order must be %s to %s (current: %s)",
                            expected, action, fulfillment.getFulfillmentStatus()),
                    "INVALID_FULFILLMENT_STATE", 409);
        }
    }

    private void emit(String eventType, FulfillmentOrder fulfillment) {
        try {
            String payload = objectMapper.writeValueAsString(new FulfillmentEventPayload(
                    fulfillment.getId(),
                    fulfillment.getMasterOrderId(),
                    fulfillment.getSellerId(),
                    fulfillment.getUnitId(),
                    fulfillment.getFulfillmentStatus().name(),
                    fulfillment.getEscrowStatus().name()
            ));
            outboxEventRepository.save(new OutboxEvent(
                    "ORDER",
                    fulfillment.getId().toString(),
                    eventType,
                    payload,
                    instanceCorrelationId,
                    eventType + ":" + fulfillment.getId() + ":" + fulfillment.getVersion()
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize %s payload for fulfillment %s", eventType, fulfillment.getId());
        }
    }

    record FulfillmentEventPayload(
            UUID fulfillmentOrderId,
            UUID masterOrderId,
            Long sellerId,
            UUID unitId,
            String fulfillmentStatus,
            String escrowStatus
    ) {}
}
