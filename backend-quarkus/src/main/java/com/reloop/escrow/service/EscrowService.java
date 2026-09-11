package com.reloop.escrow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.disputes.repository.DisputeRepository;
import com.reloop.escrow.dto.EscrowDtos;
import com.reloop.ledger.domain.FinancialJournalEntry;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.repository.FinancialJournalEntryRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read/orchestration views over the escrow contract. Per Rule 001 the escrow
 * state itself stays on fulfillment_orders.escrow_status + the double-entry
 * ledger — this module owns no tables and never duplicates settlement math:
 * release reuses the ESCROW_SETTLEMENT journal shape posted by the orders
 * module's complete() and the disputes module's RELEASE_PAYMENT path.
 */
@ApplicationScoped
public class EscrowService {
    private static final Logger log = Logger.getLogger(EscrowService.class);

    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final DisputeRepository disputeRepository;
    private final FinancialJournalEntryRepository journalRepository;
    private final DoubleEntryLedgerService ledgerService;
    private final AuditService auditService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public EscrowService(
            FulfillmentOrderRepository fulfillmentOrderRepository,
            MasterOrderRepository masterOrderRepository,
            DisputeRepository disputeRepository,
            FinancialJournalEntryRepository journalRepository,
            DoubleEntryLedgerService ledgerService,
            AuditService auditService,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.disputeRepository = disputeRepository;
        this.journalRepository = journalRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public EscrowDtos.EscrowSummaryDto getEscrowSummary(UUID fulfillmentOrderId, Long callerId, boolean admin) {
        FulfillmentOrder fulfillment = load(fulfillmentOrderId);
        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));

        if (!admin) {
            Long sellerUserId = fulfillmentOrderRepository.resolveSellerUserId(fulfillment.getSellerId());
            boolean isSeller = sellerUserId != null && sellerUserId.equals(callerId);
            boolean isBuyer = masterOrder.getBuyerId().equals(callerId);
            if (!isSeller && !isBuyer) {
                throw new BusinessException("Escrow summary is only visible to the seller, buyer or an admin",
                        "ESCROW_FORBIDDEN", 403);
            }
        }

        // Journals touching this order: escrow hold references the master order number,
        // settlements reference the fulfillment id (see OrderFulfillmentService /
        // DisputeService posting keys).
        List<FinancialJournalEntry> journals = new java.util.ArrayList<>();
        journalRepository.findByReferenceTypeAndReferenceId("ORDER_PAYMENT", masterOrder.getOrderNumber())
                .ifPresent(journals::add);
        journalRepository.findByReferenceId(fulfillment.getId().toString()).forEach(journals::add);

        List<EscrowDtos.JournalDto> journalDtos = journals.stream()
                .map(j -> new EscrowDtos.JournalDto(
                        j.getId(), j.getReferenceType(), j.getReferenceId(), j.getDescription(), j.getCreatedAt()))
                .toList();

        boolean disputeOpen = disputeRepository
                .findByFulfillmentOrderIdAndStatusOpen(fulfillmentOrderId).isPresent();

        return new EscrowDtos.EscrowSummaryDto(
                fulfillment.getId(),
                fulfillment.getMasterOrderId(),
                fulfillment.getSellerId(),
                fulfillment.getEscrowStatus().name(),
                fulfillment.getSubtotalAmount(),
                fulfillment.getPlatformFeeAmount(),
                fulfillment.getSellerNetAmount(),
                disputeOpen,
                journalDtos
        );
    }

    /**
     * Admin-forced escrow release for a HELD escrow. Mirrors the settlement
     * posting used by OrderFulfillmentService.complete() and the disputes
     * RELEASE_PAYMENT path: DR ESCROW_HELD total / CR SELLER_PAYABLE net /
     * CR PLATFORM_REVENUE fee, then escrow_status -> SETTLED.
     */
    @Transactional
    public EscrowDtos.EscrowSummaryDto releaseEscrow(UUID fulfillmentOrderId, Long adminId) {
        FulfillmentOrder fulfillment = load(fulfillmentOrderId);
        if (fulfillment.getEscrowStatus() != FulfillmentOrder.EscrowStatus.HELD) {
            throw new BusinessException("Only HELD escrow can be force-released (current: "
                    + fulfillment.getEscrowStatus() + ")", "INVALID_ESCROW_STATE", 409);
        }

        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));

        ledgerService.postJournal("ESCROW_SETTLEMENT", masterOrder.getOrderNumber(),
                "Admin-forced escrow release for order " + masterOrder.getOrderNumber(),
                List.of(
                        new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.DR, fulfillment.getSubtotalAmount()),
                        new DoubleEntryLedgerService.PostingLine("SELLER_PAYABLE:" + fulfillment.getSellerId(),
                                FinancialLedgerLine.EntryType.CR, fulfillment.getSellerNetAmount()),
                        new DoubleEntryLedgerService.PostingLine("PLATFORM_REVENUE",
                                FinancialLedgerLine.EntryType.CR, fulfillment.getPlatformFeeAmount())
                ));

        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.SETTLED);
        fulfillment = fulfillmentOrderRepository.save(fulfillment);

        auditService.record("FulfillmentOrder", fulfillment.getId().toString(), "ESCROW_FORCE_RELEASED", adminId,
                null, FulfillmentOrder.EscrowStatus.HELD.name(), FulfillmentOrder.EscrowStatus.SETTLED.name());
        emitForceReleased(fulfillment);
        log.infof("Escrow force-released for fulfillment %s by admin %s", fulfillment.getId(), adminId);
        return getEscrowSummary(fulfillment.getId(), adminId, true);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public EscrowDtos.EscrowStatsDto platformEscrowStats() {
        var query = fulfillmentOrderRepository.findAll();
        long total = query.count();
        List<FulfillmentOrder> all = query.list();
        // ponytail: full scan fine for stats; replace with GROUP BY when table grows past ~10^5 rows

        Map<String, Long> counts = new HashMap<>();
        BigDecimal held = BigDecimal.ZERO;
        for (FulfillmentOrder f : all) {
            counts.merge(f.getEscrowStatus().name(), 1L, Long::sum);
            if (f.getEscrowStatus() == FulfillmentOrder.EscrowStatus.HELD) {
                held = held.add(f.getSubtotalAmount());
            }
        }
        assert counts.values().stream().mapToLong(Long::longValue).sum() == total;
        return new EscrowDtos.EscrowStatsDto(counts, held);
    }

    private FulfillmentOrder load(UUID fulfillmentOrderId) {
        return fulfillmentOrderRepository.findByIdOptional(fulfillmentOrderId)
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));
    }

    private void emitForceReleased(FulfillmentOrder fulfillment) {
        try {
            String payload = objectMapper.writeValueAsString(new ForceReleasedPayload(
                    fulfillment.getId(),
                    fulfillment.getMasterOrderId(),
                    fulfillment.getSellerId(),
                    fulfillment.getSubtotalAmount(),
                    fulfillment.getSellerNetAmount(),
                    fulfillment.getPlatformFeeAmount(),
                    fulfillment.getEscrowStatus().name()
            ));
            outboxEventRepository.save(new OutboxEvent(
                    "ESCROW",
                    fulfillment.getId().toString(),
                    "ESCROW_FORCE_RELEASED",
                    payload,
                    instanceCorrelationId,
                    "ESCROW_FORCE_RELEASED:" + fulfillment.getId()
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize ESCROW_FORCE_RELEASED payload for fulfillment %s", fulfillment.getId());
        }
    }

    record ForceReleasedPayload(
            UUID fulfillmentOrderId,
            UUID masterOrderId,
            Long sellerId,
            BigDecimal subtotalAmount,
            BigDecimal sellerNetAmount,
            BigDecimal platformFeeAmount,
            String escrowStatus
    ) {}
}
