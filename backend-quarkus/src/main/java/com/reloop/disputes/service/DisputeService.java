package com.reloop.disputes.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import com.reloop.disputes.domain.Dispute;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.repository.DisputeRepository;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DisputeService {
    private static final Logger log = Logger.getLogger(DisputeService.class);

    private final DisputeRepository disputeRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public DisputeService(
            DisputeRepository disputeRepository,
            FulfillmentOrderRepository fulfillmentOrderRepository,
            MasterOrderRepository masterOrderRepository,
            DoubleEntryLedgerService ledgerService,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.disputeRepository = disputeRepository;
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.ledgerService = ledgerService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public DisputeDtos.DisputeResponse createDispute(Long buyerId, DisputeDtos.CreateDisputeRequest request) {
        FulfillmentOrder fulfillment = fulfillmentOrderRepository.findByIdOptional(request.fulfillmentOrderId())
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));

        // IDOR guard: only the buyer of the master order behind this fulfillment may dispute it
        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));
        if (!masterOrder.getBuyerId().equals(buyerId)) {
            throw new BusinessException("Only the buyer of this order can file a dispute", "DISPUTE_FORBIDDEN", 403);
        }
        // Anti-abuse: one open dispute per fulfillment
        if (disputeRepository.findByFulfillmentOrderIdAndStatusOpen(request.fulfillmentOrderId()).isPresent()) {
            throw new BusinessException("An open dispute already exists for this fulfillment", "DISPUTE_ALREADY_OPEN", 409);
        }

        Dispute dispute = new Dispute(
                request.fulfillmentOrderId(),
                buyerId,
                fulfillment.getSellerId(),
                request.reason(),
                request.claimDescription()
        );
        dispute = disputeRepository.save(dispute);

        // Escrow linkage: freeze the escrow for the arbitration window
        if (fulfillment.getEscrowStatus() == FulfillmentOrder.EscrowStatus.HELD) {
            fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.DISPUTED);
            fulfillmentOrderRepository.save(fulfillment);
        }
        return toResponse(dispute);
    }

    @Transactional
    public DisputeDtos.DisputeResponse resolveDispute(UUID disputeId, Long adminId, DisputeDtos.ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findByIdOptional(disputeId)
                .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", 404));

        if (dispute.getStatus() == Dispute.DisputeStatus.RESOLVED || dispute.getStatus() == Dispute.DisputeStatus.CLOSED) {
            throw new BusinessException("Dispute is already resolved", "ALREADY_RESOLVED", 409);
        }

        Dispute.ResolutionType type = Dispute.ResolutionType.valueOf(request.resolutionType().toUpperCase());

        FulfillmentOrder fulfillment = fulfillmentOrderRepository.findByIdOptional(dispute.getFulfillmentOrderId())
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));

        BigDecimal refund = request.buyerRefundAmount() != null ? request.buyerRefundAmount() : BigDecimal.ZERO;
        BigDecimal payout = request.sellerPayoutAmount() != null ? request.sellerPayoutAmount() : BigDecimal.ZERO;

        settleEscrow(dispute, type, fulfillment, refund, payout);

        dispute.setResolutionType(type);
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setBuyerRefundAmount(refund);
        dispute.setSellerPayoutAmount(payout);
        dispute.setResolutionNotes(request.resolutionNotes());
        dispute.setResolvedByAdminId(adminId);
        dispute.setResolvedAt(Instant.now());

        dispute = disputeRepository.save(dispute);
        emitDisputeResolvedEvent(dispute, type);
        return toResponse(dispute);
    }

    /**
     * Posts the balanced escrow-release journal and updates the fulfillment escrow
     * status inside the same transaction as the dispute state change, so the money
     * movement can never be recorded without the resolution (or vice versa).
     */
    private void settleEscrow(Dispute dispute, Dispute.ResolutionType type, FulfillmentOrder fulfillment,
                              BigDecimal refund, BigDecimal payout) {
        BigDecimal subtotal = fulfillment.getSubtotalAmount();

        if (type == Dispute.ResolutionType.REPAIR || type == Dispute.ResolutionType.REPLACEMENT) {
            if (fulfillment.getEscrowStatus() == FulfillmentOrder.EscrowStatus.DISPUTED) {
                fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.HELD);
                fulfillmentOrderRepository.save(fulfillment);
            }
            return; // No money movement; escrow stays HELD until the remedy completes
        }

        if (refund.add(payout).compareTo(subtotal) > 0) {
            throw new BusinessException(
                    String.format("Settlement (refund %s + payout %s) exceeds escrow subtotal %s", refund, payout, subtotal),
                    "SETTLEMENT_EXCEEDS_SUBTOTAL", 422);
        }

        switch (type) {
            case FULL_REFUND -> {
                // Entire escrow goes back to the buyer
                ledgerService.postJournal("DISPUTE_SETTLEMENT", dispute.getId().toString(),
                        "Full escrow refund for dispute " + dispute.getId(),
                        List.of(
                                new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.DR, subtotal),
                                new DoubleEntryLedgerService.PostingLine("GATEWAY_CLEARING", FinancialLedgerLine.EntryType.CR, subtotal)
                        ));
                fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.FULLY_REFUNDED);
            }
            case PARTIAL_REFUND, RELEASE_PAYMENT -> {
                BigDecimal remainder = subtotal.subtract(refund).subtract(payout);
                var lines = new java.util.ArrayList<DoubleEntryLedgerService.PostingLine>();
                lines.add(new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.DR, subtotal));
                if (refund.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(new DoubleEntryLedgerService.PostingLine("GATEWAY_CLEARING", FinancialLedgerLine.EntryType.CR, refund));
                }
                if (payout.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(new DoubleEntryLedgerService.PostingLine(
                            "SELLER_PAYABLE:" + fulfillment.getSellerId(), FinancialLedgerLine.EntryType.CR, payout));
                }
                if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(new DoubleEntryLedgerService.PostingLine("PLATFORM_REVENUE", FinancialLedgerLine.EntryType.CR, remainder));
                }
                ledgerService.postJournal("DISPUTE_SETTLEMENT", dispute.getId().toString(),
                        "Arbitrated escrow split for dispute " + dispute.getId(), lines);

                fulfillment.setEscrowStatus(type == Dispute.ResolutionType.RELEASE_PAYMENT
                        ? FulfillmentOrder.EscrowStatus.SETTLED
                        : FulfillmentOrder.EscrowStatus.PARTIALLY_REFUNDED);
            }
            default -> throw new BusinessException("Unsupported resolution type: " + type, "UNSUPPORTED_RESOLUTION", 400);
        }

        fulfillmentOrderRepository.save(fulfillment);
    }

    private void emitDisputeResolvedEvent(Dispute dispute, Dispute.ResolutionType type) {
        try {
            String payload = objectMapper.writeValueAsString(new DisputeResolvedPayload(
                    dispute.getId(),
                    dispute.getFulfillmentOrderId(),
                    dispute.getBuyerId(),
                    dispute.getSellerId(),
                    type.name(),
                    dispute.getBuyerRefundAmount(),
                    dispute.getSellerPayoutAmount()
            ));
            outboxEventRepository.save(new OutboxEvent(
                    "DISPUTE",
                    dispute.getId().toString(),
                    "DISPUTE_RESOLVED",
                    payload,
                    instanceCorrelationId,
                    "DISPUTE_RESOLVED:" + dispute.getId()
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize DISPUTE_RESOLVED payload for dispute %s", dispute.getId());
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<DisputeDtos.DisputeResponse> getBuyerDisputes(Long buyerId, int page, int size) {
        var query = disputeRepository.find("buyerId = ?1",
                io.quarkus.panache.common.Sort.descending("createdAt"), buyerId);
        long total = query.count();
        List<Dispute> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(this::toResponse).toList(), total, page, size);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<DisputeDtos.DisputeResponse> getAllDisputes(int page, int size) {
        var query = disputeRepository.findAll(io.quarkus.panache.common.Sort.descending("createdAt"));        long total = query.count();
        List<Dispute> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(this::toResponse).toList(), total, page, size);
    }

    private DisputeDtos.DisputeResponse toResponse(Dispute d) {
        return new DisputeDtos.DisputeResponse(
                d.getId(),
                d.getFulfillmentOrderId(),
                d.getBuyerId(),
                d.getSellerId(),
                d.getReason(),
                d.getClaimDescription(),
                d.getStatus().name(),
                d.getResolutionType() != null ? d.getResolutionType().name() : null,
                d.getBuyerRefundAmount(),
                d.getSellerPayoutAmount(),
                d.getCreatedAt()
        );
    }

    record DisputeResolvedPayload(
            UUID disputeId,
            UUID fulfillmentOrderId,
            Long buyerId,
            Long sellerId,
            String resolutionType,
            BigDecimal buyerRefundAmount,
            BigDecimal sellerPayoutAmount
    ) {}
}
