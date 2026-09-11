package com.reloop.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.orders.service.OrderFulfillmentService;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.support.TestFields;
import com.reloop.warranties.service.WarrantyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    private MasterOrderRepository masterOrderRepository;
    @Mock
    private com.reloop.units.repository.ProductUnitRepository productUnitRepository;
    @Mock
    private DoubleEntryLedgerService ledgerService;
    @Mock
    private WarrantyService warrantyService;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AuditService auditService;

    @Mock
    private com.reloop.ledger.repository.FinancialJournalEntryRepository journalRepository;

    private OrderFulfillmentService service;

    @BeforeEach
    void setUp() {
        service = new OrderFulfillmentService(
                fulfillmentOrderRepository,
                masterOrderRepository,
                productUnitRepository,
                ledgerService,
                warrantyService,
                outboxEventRepository,
                auditService,
                journalRepository,
                new ObjectMapper(),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        );
    }

    private FulfillmentOrder fulfillment(Long sellerId, FulfillmentOrder.FulfillmentStatus status) {
        FulfillmentOrder fulfillment = new FulfillmentOrder(
                UUID.randomUUID(), sellerId, UUID.randomUUID(),
                new BigDecimal("10000000.00"),
                new BigDecimal("1500000.00"),
                new BigDecimal("8500000.00"));
        TestFields.set(fulfillment, "id", UUID.randomUUID());
        fulfillment.setFulfillmentStatus(status);
        if (status == FulfillmentOrder.FulfillmentStatus.SHIPPED || status == FulfillmentOrder.FulfillmentStatus.DELIVERED) {
            fulfillment.setShippedAt(java.time.Instant.now());
        }
        if (status == FulfillmentOrder.FulfillmentStatus.DELIVERED) {
            fulfillment.setDeliveredAt(java.time.Instant.now());
        }
        return fulfillment;
    }

    @Test
    @DisplayName("Seller can ship own PROCESSING fulfillment; audit + outbox event recorded")
    void testShipSuccess() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.PROCESSING);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(fulfillmentOrderRepository.resolveSellerUserId(20L)).thenReturn(20L);
        when(fulfillmentOrderRepository.save(any(FulfillmentOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FulfillmentOrder result = service.ship(fulfillment.getId(), 20L, "JNE-123", "JNE");

        assertThat(result.getFulfillmentStatus()).isEqualTo(FulfillmentOrder.FulfillmentStatus.SHIPPED);
        assertThat(result.getTrackingNumber()).isEqualTo("JNE-123");
        assertThat(result.getShippedAt()).isNotNull();
        verify(outboxEventRepository).save(any());
        verify(auditService).record(eq("FulfillmentOrder"), anyString(), eq("STATE_TRANSITION"),
                eq(20L), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Ship rejected when the fulfillment belongs to another seller")
    void testShipWrongOwner() {
        FulfillmentOrder fulfillment = fulfillment(21L, FulfillmentOrder.FulfillmentStatus.PROCESSING);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(fulfillmentOrderRepository.resolveSellerUserId(21L)).thenReturn(99L); // another user owns the store

        assertThatThrownBy(() -> service.ship(fulfillment.getId(), 20L, "JNE-123", "JNE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to this seller");
    }

    @Test
    @DisplayName("Ship rejected when fulfillment is not in PROCESSING state")
    void testShipInvalidState() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.DELIVERED);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(fulfillmentOrderRepository.resolveSellerUserId(20L)).thenReturn(20L);

        assertThatThrownBy(() -> service.ship(fulfillment.getId(), 20L, "JNE-123", "JNE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be PROCESSING");
    }

    @Test
    @DisplayName("Complete settles escrow (seller net + platform fee) and issues warranty")
    void testCompleteSuccess() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.DELIVERED);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        MasterOrder masterOrder = new MasterOrder("ORD-1", 10L, new BigDecimal("10000000.00"), "{}");
        when(masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())).thenReturn(Optional.of(masterOrder));
        when(fulfillmentOrderRepository.save(any(FulfillmentOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        com.reloop.units.domain.ProductUnit unit = new com.reloop.units.domain.ProductUnit(
                java.util.UUID.randomUUID(), "SN-1", 20L,
                com.reloop.units.domain.ProductUnit.UnitStatus.SOLD, "A");
        TestFields.set(unit, "id", fulfillment.getUnitId());
        when(productUnitRepository.findByIdOptional(fulfillment.getUnitId())).thenReturn(Optional.of(unit));
        when(productUnitRepository.save(any(com.reloop.units.domain.ProductUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FulfillmentOrder result = service.complete(fulfillment.getId());

        assertThat(result.getFulfillmentStatus()).isEqualTo(FulfillmentOrder.FulfillmentStatus.COMPLETED);
        assertThat(result.getEscrowStatus()).isEqualTo(FulfillmentOrder.EscrowStatus.SETTLED);

        var linesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(ledgerService).postJournal(eq("ESCROW_SETTLEMENT"), eq("ORD-1"), anyString(),
                linesCaptor.capture());
        assertThat(linesCaptor.getValue()).hasSize(3);

        // Ownership + custody transferred to the buyer
        assertThat(unit.getCurrentOwnerId()).isEqualTo(10L);
        assertThat(unit.getCurrentCustody()).isEqualTo(com.reloop.units.domain.ProductUnit.PhysicalCustody.BUYER);
        assertThat(unit.getStatus()).isEqualTo(com.reloop.units.domain.ProductUnit.UnitStatus.OWNED);

        verify(warrantyService).issueStandardWarranty(fulfillment.getUnitId(), 10L, fulfillment.getId());
        verify(outboxEventRepository).save(any());
        verify(auditService).record(anyString(), anyString(), eq("STATE_TRANSITION"), any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Complete rejected when fulfillment is not DELIVERED")
    void testCompleteInvalidState() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.PROCESSING);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));

        assertThatThrownBy(() -> service.complete(fulfillment.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be DELIVERED");
    }

    @Test
    @DisplayName("Payout moves SELLER_PAYABLE to DISBURSEMENT_BANK with audit + outbox")
    void testPayoutSuccess() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.COMPLETED);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(journalRepository.findByReferenceTypeAndReferenceId("SELLER_PAYOUT", fulfillment.getId().toString()))
                .thenReturn(Optional.empty());

        FulfillmentOrder result = service.disbursePayout(fulfillment.getId());

        var linesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(ledgerService).postJournal(eq("SELLER_PAYOUT"), eq(fulfillment.getId().toString()), anyString(),
                linesCaptor.capture());
        assertThat(linesCaptor.getValue()).hasSize(2);
        verify(outboxEventRepository).save(any());
        verify(auditService).record(anyString(), anyString(), eq("PAYOUT"), any(), any(), anyString(), anyString());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Payout is idempotent: a second attempt is rejected with 409")
    void testPayoutAlreadyDisbursed() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.COMPLETED);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(journalRepository.findByReferenceTypeAndReferenceId("SELLER_PAYOUT", fulfillment.getId().toString()))
                .thenReturn(Optional.of(new com.reloop.ledger.domain.FinancialJournalEntry(
                        "SELLER_PAYOUT", fulfillment.getId().toString(), "existing")));

        assertThatThrownBy(() -> service.disbursePayout(fulfillment.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already disbursed");
    }

    @Test
    @DisplayName("Payout rejected when fulfillment is not COMPLETED")
    void testPayoutInvalidState() {
        FulfillmentOrder fulfillment = fulfillment(20L, FulfillmentOrder.FulfillmentStatus.SHIPPED);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));

        assertThatThrownBy(() -> service.disbursePayout(fulfillment.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("Admin feed pages all fulfillments regardless of seller")
    void testGetAllFulfillments() {
        List<FulfillmentOrder> rows = List.of(
                fulfillment(20L, FulfillmentOrder.FulfillmentStatus.PROCESSING),
                fulfillment(21L, FulfillmentOrder.FulfillmentStatus.SHIPPED));
        var query = org.mockito.Mockito.mock(io.quarkus.hibernate.orm.panache.PanacheQuery.class);
        when(fulfillmentOrderRepository.findAll(any(io.quarkus.panache.common.Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(2L);
        when(query.page(any(io.quarkus.panache.common.Page.class))).thenReturn(query);
        when(query.list()).thenReturn(rows);

        var result = service.getAllFulfillments(0, 20);

        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).extracting(FulfillmentOrder::getSellerId).containsExactlyInAnyOrder(20L, 21L);
    }
}
