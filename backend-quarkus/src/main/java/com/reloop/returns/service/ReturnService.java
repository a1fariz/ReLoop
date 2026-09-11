package com.reloop.returns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.notifications.service.NotificationService;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.returns.domain.ReturnAuthorization;
import com.reloop.returns.dto.ReturnDtos;
import com.reloop.returns.repository.ReturnAuthorizationRepository;
import com.reloop.warranties.repository.WarrantyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Return authorization lifecycle: REQUESTED → APPROVED/REJECTED → IN_TRANSIT →
 * RECEIVED → INSPECTED → REFUNDED → CLOSED. Refund finalization mirrors the
 * dispute settlement journal (DR ESCROW_HELD / CR GATEWAY_CLEARING) and flips
 * the fulfillment escrow status, in the same transaction as the state change.
 */
@ApplicationScoped
public class ReturnService {
    private static final Logger log = Logger.getLogger(ReturnService.class);

    private final ReturnAuthorizationRepository returnRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final WarrantyRepository warrantyRepository;
    private final NotificationService notificationService;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public ReturnService(
            ReturnAuthorizationRepository returnRepository,
            FulfillmentOrderRepository fulfillmentOrderRepository,
            MasterOrderRepository masterOrderRepository,
            DoubleEntryLedgerService ledgerService,
            WarrantyRepository warrantyRepository,
            NotificationService notificationService,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.returnRepository = returnRepository;
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.ledgerService = ledgerService;
        this.warrantyRepository = warrantyRepository;
        this.notificationService = notificationService;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public ReturnDtos.ReturnResponse requestReturn(Long buyerId, ReturnDtos.CreateReturnRequest request) {
        FulfillmentOrder fulfillment = fulfillmentOrderRepository.findByIdOptional(request.fulfillmentOrderId())
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));

        // IDOR guard: only the buyer of the master order behind this fulfillment may request a return
        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));
        if (!masterOrder.getBuyerId().equals(buyerId)) {
            throw new BusinessException("Only the buyer of this order can request a return", "RETURN_FORBIDDEN", 403);
        }

        // Return window: the item must actually be in the buyer's hands
        if (fulfillment.getFulfillmentStatus() != FulfillmentOrder.FulfillmentStatus.DELIVERED
                && fulfillment.getFulfillmentStatus() != FulfillmentOrder.FulfillmentStatus.COMPLETED) {
            throw new BusinessException("Returns require a DELIVERED or COMPLETED fulfillment", "RETURN_WINDOW_INVALID", 409);
        }

        if (returnRepository.existsOpenByFulfillmentOrderId(request.fulfillmentOrderId())) {
            throw new BusinessException("An open return already exists for this fulfillment", "RETURN_ALREADY_OPEN", 409);
        }

        String evidenceJson;
        try {
            evidenceJson = objectMapper.writeValueAsString(
                    request.evidenceImages() != null ? request.evidenceImages() : List.of());
        } catch (JsonProcessingException e) {
            throw new BusinessException("Invalid evidence images payload", "INVALID_EVIDENCE", 422);
        }

        ReturnAuthorization ret = new ReturnAuthorization(
                request.fulfillmentOrderId(), buyerId,
                request.reason(), request.description(), evidenceJson);
        ret = returnRepository.save(ret);

        auditService.record("ReturnAuthorization", ret.getId().toString(), "CREATE", buyerId,
                null, null, ReturnAuthorization.Status.REQUESTED.name());
        emit("RETURN_REQUESTED", ret, null);
        return toResponse(ret);
    }

    @Transactional
    public ReturnDtos.ReturnResponse review(UUID returnId, Long adminId, ReturnDtos.ReviewReturnRequest request) {
        ReturnAuthorization ret = load(returnId);
        requireStatus(ret, ReturnAuthorization.Status.REQUESTED, "review");

        if (request.approved()) {
            ret.setStatus(ReturnAuthorization.Status.APPROVED);
        } else {
            ret.setStatus(ReturnAuthorization.Status.REJECTED);
            ret.setRejectionReason(request.reason());
            ret.setResolvedAt(Instant.now());
        }
        ret = returnRepository.save(ret);

        auditService.record("ReturnAuthorization", ret.getId().toString(), "STATE_TRANSITION", adminId,
                null, ReturnAuthorization.Status.REQUESTED.name(), ret.getStatus().name());
        emit(request.approved() ? "RETURN_APPROVED" : "RETURN_REJECTED", ret, null);
        notifyBuyer(ret, ret.getStatus() == ReturnAuthorization.Status.APPROVED
                ? "Return approved"
                : "Return rejected: " + (request.reason() != null ? request.reason() : "not eligible"));
        return toResponse(ret);
    }

    @Transactional
    public ReturnDtos.ReturnResponse recordShipment(UUID returnId, Long adminId, ReturnDtos.ShipmentRequest request) {
        ReturnAuthorization ret = load(returnId);
        requireStatus(ret, ReturnAuthorization.Status.APPROVED, "record shipment");

        ret.setCourierName(request.courierName());
        ret.setTrackingNumber(request.trackingNumber());
        ret.setStatus(ReturnAuthorization.Status.IN_TRANSIT);
        ret = returnRepository.save(ret);

        auditService.record("ReturnAuthorization", ret.getId().toString(), "STATE_TRANSITION", adminId,
                null, ReturnAuthorization.Status.APPROVED.name(), ReturnAuthorization.Status.IN_TRANSIT.name());
        emit("RETURN_IN_TRANSIT", ret, null);
        return toResponse(ret);
    }

    @Transactional
    public ReturnDtos.ReturnResponse recordReceived(UUID returnId, Long adminId) {
        ReturnAuthorization ret = load(returnId);
        requireStatus(ret, ReturnAuthorization.Status.IN_TRANSIT, "receive");

        ret.setStatus(ReturnAuthorization.Status.RECEIVED);
        ret = returnRepository.save(ret);

        auditService.record("ReturnAuthorization", ret.getId().toString(), "STATE_TRANSITION", adminId,
                null, ReturnAuthorization.Status.IN_TRANSIT.name(), ReturnAuthorization.Status.RECEIVED.name());
        emit("RETURN_RECEIVED", ret, null);
        return toResponse(ret);
    }

    @Transactional
    public ReturnDtos.ReturnResponse recordInspection(UUID returnId, Long adminId, ReturnDtos.InspectionRequest request) {
        ReturnAuthorization ret = load(returnId);
        requireStatus(ret, ReturnAuthorization.Status.RECEIVED, "inspect");

        FulfillmentOrder fulfillment = loadFulfillment(ret.getFulfillmentOrderId());
        BigDecimal subtotal = fulfillment.getSubtotalAmount();
        if (request.refundAmount().compareTo(BigDecimal.ZERO) < 0 || request.refundAmount().compareTo(subtotal) > 0) {
            throw new BusinessException(
                    String.format("Refund %s must be between 0 and the fulfillment subtotal %s", request.refundAmount(), subtotal),
                    "REFUND_AMOUNT_OUT_OF_RANGE", 409);
        }

        ret.setInspectionNotes(request.notes());
        ret.setRefundAmount(request.refundAmount());
        ret.setStatus(ReturnAuthorization.Status.INSPECTED);
        ret = returnRepository.save(ret);

        auditService.record("ReturnAuthorization", ret.getId().toString(), "STATE_TRANSITION", adminId,
                null, ReturnAuthorization.Status.RECEIVED.name(), ReturnAuthorization.Status.INSPECTED.name());
        emit("RETURN_INSPECTED", ret, null);
        return toResponse(ret);
    }

    @Transactional
    public ReturnDtos.ReturnResponse finalizeRefund(UUID returnId, Long adminId) {
        ReturnAuthorization ret = load(returnId);
        requireStatus(ret, ReturnAuthorization.Status.INSPECTED, "finalize refund");

        FulfillmentOrder fulfillment = loadFulfillment(ret.getFulfillmentOrderId());
        BigDecimal refund = ret.getRefundAmount();
        BigDecimal subtotal = fulfillment.getSubtotalAmount();

        // Same journal shape as the dispute settlement: escrow back to the buyer
        // (full refund) or split refund/remainder (partial refund).
        List<DoubleEntryLedgerService.PostingLine> lines = new java.util.ArrayList<>();
        lines.add(new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.DR, subtotal));
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new DoubleEntryLedgerService.PostingLine("GATEWAY_CLEARING", FinancialLedgerLine.EntryType.CR, refund));
        }
        BigDecimal remainder = subtotal.subtract(refund);
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new DoubleEntryLedgerService.PostingLine("PLATFORM_REVENUE", FinancialLedgerLine.EntryType.CR, remainder));
        }
        ledgerService.postJournal("RETURN_REFUND", ret.getId().toString(),
                "Return refund for return " + ret.getId(), lines);

        boolean fullRefund = refund.compareTo(subtotal) == 0;
        fulfillment.setEscrowStatus(fullRefund
                ? FulfillmentOrder.EscrowStatus.FULLY_REFUNDED
                : FulfillmentOrder.EscrowStatus.PARTIALLY_REFUNDED);
        fulfillmentOrderRepository.save(fulfillment);

        ret.setStatus(ReturnAuthorization.Status.REFUNDED);
        ret.setResolvedAt(Instant.now());
        ret = returnRepository.save(ret);

        // The unit has been shipped back: its active warranty must be voided.
        // ponytail: inline void via WarrantyRepository; WarrantyService exposes no void
        // method and existing files must not be modified — move into WarrantyService when returns grow
        warrantyRepository.findByUnitIdAndIsVoidedFalse(fulfillment.getUnitId()).ifPresent(warranty -> {
            warranty.setVoided(true);
            warrantyRepository.save(warranty);
        });

        auditService.record("ReturnAuthorization", ret.getId().toString(), "REFUND", adminId,
                null, ReturnAuthorization.Status.INSPECTED.name(), ReturnAuthorization.Status.REFUNDED.name());
        emit("RETURN_REFUNDED", ret, refund);
        notifyBuyer(ret, "Return refunded: IDR " + refund.toPlainString());
        return toResponse(ret);
    }

    @Transactional
    public ReturnDtos.ReturnResponse close(UUID returnId, Long adminId) {
        ReturnAuthorization ret = load(returnId);
        if (ret.getStatus() != ReturnAuthorization.Status.REFUNDED && ret.getStatus() != ReturnAuthorization.Status.REJECTED) {
            throw new BusinessException("Only REFUNDED or REJECTED returns can be closed", "INVALID_RETURN_STATE", 409);
        }

        ReturnAuthorization.Status from = ret.getStatus();
        ret.setStatus(ReturnAuthorization.Status.CLOSED);
        ret = returnRepository.save(ret);

        auditService.record("ReturnAuthorization", ret.getId().toString(), "STATE_TRANSITION", adminId,
                null, from.name(), ReturnAuthorization.Status.CLOSED.name());
        emit("RETURN_CLOSED", ret, null);
        return toResponse(ret);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ReturnDtos.ReturnResponse getReturn(UUID returnId, Long callerId, boolean isAdmin) {
        ReturnAuthorization ret = load(returnId);
        if (!ret.getBuyerId().equals(callerId) && !isAdmin) {
            // Not the buyer and not an admin: silent 404 (no existence leak)
            throw new BusinessException("Return not found", "RETURN_NOT_FOUND", 404);
        }
        return toResponse(ret);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<ReturnDtos.ReturnResponse> myReturns(Long buyerId, int page, int size) {
        var query = returnRepository.find("buyerId = ?1",
                io.quarkus.panache.common.Sort.descending("createdAt"), buyerId);
        long total = query.count();
        List<ReturnAuthorization> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(this::toResponse).toList(), total, page, size);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<ReturnDtos.ReturnResponse> adminReturns(String status, int page, int size) {
        var query = (status == null || status.isBlank())
                ? returnRepository.findAll(io.quarkus.panache.common.Sort.descending("createdAt"))
                : returnRepository.find("status = ?1", parseStatus(status));
        long total = query.count();
        List<ReturnAuthorization> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(this::toResponse).toList(), total, page, size);
    }

    private static ReturnAuthorization.Status parseStatus(String status) {
        try {
            return ReturnAuthorization.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown return status filter: " + status, "INVALID_STATUS_FILTER", 400);
        }
    }

    private ReturnAuthorization load(UUID returnId) {
        return returnRepository.findByIdOptional(returnId)
                .orElseThrow(() -> new BusinessException("Return not found", "RETURN_NOT_FOUND", 404));
    }

    private FulfillmentOrder loadFulfillment(UUID fulfillmentOrderId) {
        return fulfillmentOrderRepository.findByIdOptional(fulfillmentOrderId)
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));
    }

    private void requireStatus(ReturnAuthorization ret, ReturnAuthorization.Status expected, String action) {
        if (ret.getStatus() != expected) {
            throw new BusinessException(
                    String.format("Return must be %s to %s (current: %s)", expected, action, ret.getStatus()),
                    "INVALID_RETURN_STATE", 409);
        }
    }

    private void emit(String eventType, ReturnAuthorization ret, BigDecimal refundAmount) {
        try {
            String payload = objectMapper.writeValueAsString(new ReturnEventPayload(
                    ret.getId(), ret.getFulfillmentOrderId(), ret.getBuyerId(), ret.getStatus().name(),
                    refundAmount != null ? refundAmount : ret.getRefundAmount()));
            outboxEventRepository.save(new OutboxEvent(
                    "RETURN",
                    ret.getId().toString(),
                    eventType,
                    payload,
                    instanceCorrelationId,
                    eventType + ":" + ret.getId() + ":" + ret.getVersion()
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize %s payload for return %s", eventType, ret.getId());
        }
    }

    private void notifyBuyer(ReturnAuthorization ret, String message) {
        try {
            notificationService.push(ret.getBuyerId(),
                    com.reloop.notifications.domain.Notification.Category.RETURN,
                    "Return " + ret.getStatus().name(),
                    message, "RETURN", ret.getId());
        } catch (Exception e) {
            // In-app notification is best-effort: never blocks the return state change
            log.warnf(e, "Failed to notify buyer for return %s", ret.getId());
        }
    }

    private ReturnDtos.ReturnResponse toResponse(ReturnAuthorization r) {
        List<String> evidence;
        try {
            evidence = objectMapper.readValue(r.getEvidenceImages() != null ? r.getEvidenceImages() : "[]",
                    new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            evidence = List.of();
        }
        return new ReturnDtos.ReturnResponse(
                r.getId(),
                r.getFulfillmentOrderId(),
                r.getBuyerId(),
                r.getReason(),
                r.getDescription(),
                evidence,
                r.getStatus().name(),
                r.getRejectionReason(),
                r.getCourierName(),
                r.getTrackingNumber(),
                r.getInspectionNotes(),
                r.getRefundAmount(),
                r.getResolvedAt(),
                r.getCreatedAt()
        );
    }

    record ReturnEventPayload(
            UUID returnId,
            UUID fulfillmentOrderId,
            Long buyerId,
            String status,
            BigDecimal refundAmount
    ) {}
}
