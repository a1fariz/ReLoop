package com.reloop.returns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import com.reloop.notifications.service.NotificationService;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.returns.domain.ReturnAuthorization;
import com.reloop.returns.dto.ReturnDtos;
import com.reloop.returns.repository.ReturnAuthorizationRepository;
import com.reloop.returns.service.ReturnService;
import com.reloop.warranties.repository.WarrantyRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnAuthorizationRepository returnRepository;
    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    private MasterOrderRepository masterOrderRepository;
    @Mock
    private DoubleEntryLedgerService ledgerService;
    @Mock
    private WarrantyRepository warrantyRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AuditService auditService;

    private ReturnService returnService;

    @BeforeEach
    void setUp() {
        returnService = new ReturnService(
                returnRepository,
                fulfillmentOrderRepository,
                masterOrderRepository,
                ledgerService,
                warrantyRepository,
                notificationService,
                outboxEventRepository,
                auditService,
                new ObjectMapper()
        );
    }

    private FulfillmentOrder deliveredOrder(BigDecimal subtotal) {
        FulfillmentOrder fulfillment = new FulfillmentOrder(
                UUID.randomUUID(), 20L, UUID.randomUUID(),
                subtotal,
                subtotal.multiply(new BigDecimal("0.15")),
                subtotal.multiply(new BigDecimal("0.85"))
        );
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.DELIVERED);
        fulfillment.setEscrowStatus(FulfillmentOrder.EscrowStatus.HELD);
        return fulfillment;
    }

    private void stubOwnership(FulfillmentOrder fulfillment) {
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId()))
                .thenReturn(Optional.of(new MasterOrder("ORD-1", 10L, fulfillment.getSubtotalAmount(), "{}")));
    }

    @Test
    @DisplayName("Buyer of a DELIVERED fulfillment can request a return; created as REQUESTED")
    void testRequestReturnHappyPath() {
        UUID fulfillmentId = UUID.randomUUID();
        FulfillmentOrder fulfillment = deliveredOrder(new BigDecimal("15000000.00"));
        com.reloop.support.TestFields.set(fulfillment, "id", fulfillmentId);
        stubOwnership(fulfillment);
        when(returnRepository.existsOpenByFulfillmentOrderId(fulfillmentId)).thenReturn(false);
        when(returnRepository.save(any(ReturnAuthorization.class))).thenAnswer(invocation -> {
            ReturnAuthorization r = invocation.getArgument(0);
            com.reloop.support.TestFields.set(r, "id", UUID.randomUUID());
            return r;
        });

        var request = new ReturnDtos.CreateReturnRequest(fulfillmentId, "NOT_AS_DESCRIBED", "item differs from listing",
                List.of("https://cdn.reloop.id/evidence/1.jpg"));
        var response = returnService.requestReturn(10L, request);

        assertThat(response.status()).isEqualTo("REQUESTED");
        assertThat(response.buyerId()).isEqualTo(10L);
        assertThat(response.evidenceImages()).containsExactly("https://cdn.reloop.id/evidence/1.jpg");
        verify(outboxEventRepository).save(any());
        verify(auditService).record(eq("ReturnAuthorization"), any(), eq("CREATE"), eq(10L), any(), any(), eq("REQUESTED"));
    }

    @Test
    @DisplayName("Return request rejected when fulfillment is not DELIVERED or COMPLETED (409 RETURN_WINDOW_INVALID)")
    void testRequestReturnWindowInvalid() {
        UUID fulfillmentId = UUID.randomUUID();
        FulfillmentOrder fulfillment = deliveredOrder(new BigDecimal("15000000.00"));
        com.reloop.support.TestFields.set(fulfillment, "id", fulfillmentId);
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.SHIPPED);
        stubOwnership(fulfillment);

        var request = new ReturnDtos.CreateReturnRequest(fulfillmentId, "NOT_AS_DESCRIBED", "item differs", null);
        assertThatThrownBy(() -> returnService.requestReturn(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DELIVERED or COMPLETED");
    }

    @Test
    @DisplayName("Inspection refund above the fulfillment subtotal is rejected (409 REFUND_AMOUNT_OUT_OF_RANGE)")
    void testRefundBoundsGuard() {
        UUID returnId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        ReturnAuthorization ret = new ReturnAuthorization(fulfillmentId, 10L, "NOT_AS_DESCRIBED", "details", "[]");
        com.reloop.support.TestFields.set(ret, "id", returnId);
        ret.setStatus(ReturnAuthorization.Status.RECEIVED);

        FulfillmentOrder fulfillment = deliveredOrder(new BigDecimal("15000000.00"));
        com.reloop.support.TestFields.set(fulfillment, "id", fulfillmentId);

        when(returnRepository.findByIdOptional(returnId)).thenReturn(Optional.of(ret));
        when(fulfillmentOrderRepository.findByIdOptional(fulfillmentId)).thenReturn(Optional.of(fulfillment));

        var request = new ReturnDtos.InspectionRequest("scratches confirmed", new BigDecimal("16000000.00"));
        assertThatThrownBy(() -> returnService.recordInspection(returnId, 999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 0 and the fulfillment subtotal");
    }

    @Test
    @DisplayName("Finalize refund posts the escrow journal and flips escrow status to FULLY_REFUNDED")
    void testFinalizeFullRefund() {
        UUID returnId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        ReturnAuthorization ret = new ReturnAuthorization(fulfillmentId, 10L, "NOT_AS_DESCRIBED", "details", "[]");
        com.reloop.support.TestFields.set(ret, "id", returnId);
        ret.setStatus(ReturnAuthorization.Status.INSPECTED);
        ret.setRefundAmount(new BigDecimal("15000000.00"));

        FulfillmentOrder fulfillment = deliveredOrder(new BigDecimal("15000000.00"));
        com.reloop.support.TestFields.set(fulfillment, "id", fulfillmentId);
        com.reloop.support.TestFields.set(fulfillment, "unitId", unitId);

        when(returnRepository.findByIdOptional(returnId)).thenReturn(Optional.of(ret));
        when(fulfillmentOrderRepository.findByIdOptional(fulfillmentId)).thenReturn(Optional.of(fulfillment));
        when(returnRepository.save(any(ReturnAuthorization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fulfillmentOrderRepository.save(any(FulfillmentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warrantyRepository.findByUnitIdAndIsVoidedFalse(unitId)).thenReturn(Optional.empty());
        doAnswer(invocation -> null).when(notificationService)
                .push(any(), any(), any(), any(), any(), any());

        var response = returnService.finalizeRefund(returnId, 999L);

        assertThat(response.status()).isEqualTo("REFUNDED");
        assertThat(fulfillment.getEscrowStatus()).isEqualTo(FulfillmentOrder.EscrowStatus.FULLY_REFUNDED);
        verify(ledgerService).postJournal(eq("RETURN_REFUND"), any(), any(), any());
        verify(outboxEventRepository).save(any());
    }
}
