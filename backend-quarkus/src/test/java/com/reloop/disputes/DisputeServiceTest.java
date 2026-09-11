package com.reloop.disputes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import com.reloop.disputes.domain.Dispute;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.repository.DisputeRepository;
import com.reloop.disputes.service.DisputeService;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.outbox.repository.OutboxEventRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    private com.reloop.orders.repository.MasterOrderRepository masterOrderRepository;
    @Mock
    private DoubleEntryLedgerService ledgerService;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private DisputeService disputeService;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(
                disputeRepository,
                fulfillmentOrderRepository,
                masterOrderRepository,
                ledgerService,
                outboxEventRepository,
                new ObjectMapper()
        );
    }

    private FulfillmentOrder escrowedOrder(BigDecimal subtotal) {
        return new FulfillmentOrder(
                UUID.randomUUID(), 20L, UUID.randomUUID(),
                subtotal,
                subtotal.multiply(new BigDecimal("0.15")),
                subtotal.multiply(new BigDecimal("0.85"))
        );
    }

    @Test
    @DisplayName("Resolve with partial refund posts balanced escrow journal and updates escrow status")
    void testPartialRefundResolution() {
        UUID disputeId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        Dispute dispute = new Dispute(fulfillmentId, 10L, 20L, "DAMAGED_SCREEN", "Screen has unlisted scratches");
        com.reloop.support.TestFields.set(dispute, "id", disputeId);
        FulfillmentOrder fulfillment = escrowedOrder(new BigDecimal("15000000.00"));
        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.HELD);

        when(disputeRepository.findByIdOptional(disputeId)).thenReturn(Optional.of(dispute));
        when(fulfillmentOrderRepository.findByIdOptional(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new DisputeDtos.ResolveDisputeRequest(
                "PARTIAL_REFUND",
                new BigDecimal("500000.00"),   // 500k refund to buyer
                new BigDecimal("14500000.00"), // 14.5M net release to seller
                "Agreed on partial compensation for cosmetic scratch"
        );

        var response = disputeService.resolveDispute(disputeId, 999L, request);

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(response.resolutionType()).isEqualTo("PARTIAL_REFUND");
        assertThat(response.buyerRefundAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(response.sellerPayoutAmount()).isEqualByComparingTo(new BigDecimal("14500000.00"));
        assertThat(fulfillment.getEscrowStatus()).isEqualTo(FulfillmentOrder.EscrowStatus.PARTIALLY_REFUNDED);

        // 500k refund + 14.5M payout == 15M subtotal: fully balanced, no remainder
        verify(ledgerService).postJournal(any(), any(), any(), any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Reject settlement when refund + payout exceed the escrow subtotal")
    void testRejectSettlementOverSubtotal() {
        UUID disputeId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        Dispute dispute = new Dispute(fulfillmentId, 10L, 20L, "DAMAGED", "details");
        FulfillmentOrder fulfillment = escrowedOrder(new BigDecimal("15000000.00"));

        when(disputeRepository.findByIdOptional(disputeId)).thenReturn(Optional.of(dispute));
        when(fulfillmentOrderRepository.findByIdOptional(fulfillmentId)).thenReturn(Optional.of(fulfillment));

        var request = new DisputeDtos.ResolveDisputeRequest(
                "PARTIAL_REFUND",
                new BigDecimal("1000000.00"),
                new BigDecimal("15000000.00"), // 1M + 15M > 15M subtotal
                "notes");

        assertThatThrownBy(() -> disputeService.resolveDispute(disputeId, 999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds escrow subtotal");
    }

    @Test
    @DisplayName("Reject resolving an already resolved dispute")
    void testRejectAlreadyResolvedDispute() {
        UUID disputeId = UUID.randomUUID();
        Dispute dispute = new Dispute(UUID.randomUUID(), 10L, 20L, "DAMAGED", "details");
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);

        when(disputeRepository.findByIdOptional(disputeId)).thenReturn(Optional.of(dispute));

        var request = new DisputeDtos.ResolveDisputeRequest("FULL_REFUND", new BigDecimal("1000.00"), BigDecimal.ZERO, "notes");

        assertThatThrownBy(() -> disputeService.resolveDispute(disputeId, 999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already resolved");
    }

    @Test
    @DisplayName("Admin feed pages all disputes mapped to responses")
    void testGetAllDisputes() {
        Dispute first = new Dispute(UUID.randomUUID(), 10L, 20L, "DAMAGED", "d1");
        Dispute second = new Dispute(UUID.randomUUID(), 11L, 21L, "NOT_AS_DESCRIBED", "d2");
        com.reloop.support.TestFields.set(first, "id", UUID.randomUUID());
        com.reloop.support.TestFields.set(second, "id", UUID.randomUUID());

        var query = org.mockito.Mockito.mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(disputeRepository.findAll(any(io.quarkus.panache.common.Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(2L);
        when(query.page(any(io.quarkus.panache.common.Page.class))).thenReturn(query);
        when(query.list()).thenReturn(java.util.List.of(first, second));

        var result = disputeService.getAllDisputes(0, 20);

        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.items()).extracting(DisputeDtos.DisputeResponse::reason).containsExactly("DAMAGED", "NOT_AS_DESCRIBED");
    }

    private void stubOwnership(UUID fulfillmentId, Long fulfillmentBuyerId) {
        FulfillmentOrder fulfillment = escrowedOrder(new BigDecimal("15000000.00"));
        com.reloop.support.TestFields.set(fulfillment, "id", fulfillmentId);
        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.HELD);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId()))
                .thenReturn(Optional.of(new MasterOrder("ORD-1", fulfillmentBuyerId, new BigDecimal("15000000.00"), "{}")));
        org.mockito.Mockito.lenient().when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(fulfillmentOrderRepository.save(any(FulfillmentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Buyer of the order can file a dispute; escrow freezes to DISPUTED")
    void testCreateDisputeFreezesEscrow() {
        UUID fulfillmentId = UUID.randomUUID();
        stubOwnership(fulfillmentId, 10L);
        when(disputeRepository.findByFulfillmentOrderIdAndStatusOpen(fulfillmentId)).thenReturn(Optional.empty());

        var request = new DisputeDtos.CreateDisputeRequest(fulfillmentId, 20L, "DAMAGED_SCREEN", "screen cracks");
        var response = disputeService.createDispute(10L, request);

        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.buyerId()).isEqualTo(10L);
        var captor = org.mockito.ArgumentCaptor.forClass(FulfillmentOrder.class);
        verify(fulfillmentOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getEscrowStatus()).isEqualTo(FulfillmentOrder.EscrowStatus.DISPUTED);
    }

    @Test
    @DisplayName("IDOR: a different user cannot dispute someone else's fulfillment (403)")
    void testCreateDisputeIdorBlocked() {
        UUID fulfillmentId = UUID.randomUUID();
        stubOwnership(fulfillmentId, 10L);

        var request = new DisputeDtos.CreateDisputeRequest(fulfillmentId, 20L, "DAMAGED", "not my order");
        assertThatThrownBy(() -> disputeService.createDispute(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only the buyer");
    }

    @Test
    @DisplayName("Only one open dispute per fulfillment (409)")
    void testCreateDisputeDuplicateBlocked() {
        UUID fulfillmentId = UUID.randomUUID();
        stubOwnership(fulfillmentId, 10L);
        when(disputeRepository.findByFulfillmentOrderIdAndStatusOpen(fulfillmentId))
                .thenReturn(Optional.of(new Dispute(fulfillmentId, 10L, 20L, "DAMAGED", "first")));

        var request = new DisputeDtos.CreateDisputeRequest(fulfillmentId, 20L, "DAMAGED", "second attempt");
        assertThatThrownBy(() -> disputeService.createDispute(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("REPAIR resolution returns a frozen escrow to HELD without money movement")
    void testRepairResolutionUnfreezesEscrow() {
        UUID disputeId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        Dispute dispute = new Dispute(fulfillmentId, 10L, 20L, "DAMAGED", "details");
        com.reloop.support.TestFields.set(dispute, "id", disputeId);
        FulfillmentOrder fulfillment = escrowedOrder(new BigDecimal("15000000.00"));
        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.DISPUTED);

        when(disputeRepository.findByIdOptional(disputeId)).thenReturn(Optional.of(dispute));
        when(fulfillmentOrderRepository.findByIdOptional(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fulfillmentOrderRepository.save(any(FulfillmentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new DisputeDtos.ResolveDisputeRequest("REPAIR", null, null, "repair in progress");
        var response = disputeService.resolveDispute(disputeId, 999L, request);

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(response.resolutionType()).isEqualTo("REPAIR");
        assertThat(fulfillment.getEscrowStatus()).isEqualTo(FulfillmentOrder.EscrowStatus.HELD);
        verify(ledgerService, org.mockito.Mockito.never()).postJournal(any(), any(), any(), any());
    }
}
