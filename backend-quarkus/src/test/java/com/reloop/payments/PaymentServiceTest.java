package com.reloop.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.payments.domain.PaymentAttempt;
import com.reloop.payments.dto.PaymentDtos;
import com.reloop.payments.repository.PaymentAttemptRepository;
import com.reloop.payments.service.PaymentService;
import com.reloop.support.TestFields;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private MasterOrderRepository masterOrderRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentAttemptRepository,
                masterOrderRepository,
                auditService,
                outboxEventRepository,
                objectMapper
        );
    }

    private MasterOrder masterOrder(Long buyerId, String total, boolean paid) {
        MasterOrder order = new MasterOrder("ORD-TEST-001", buyerId, new BigDecimal(total), "\"Jl. Test\"");
        if (paid) {
            order.setPaymentStatus(MasterOrder.PaymentStatus.PAID);
        }
        return order;
    }

    @Test
    @DisplayName("Initiate payment: PROCESSING attempt created from master order total, audit + outbox emitted")
    void testInitiatePaymentHappyPath() {
        UUID masterOrderId = UUID.randomUUID();
        Long buyerId = 1L;
        MasterOrder order = masterOrder(buyerId, "10000000.00", false);

        when(paymentAttemptRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(masterOrderRepository.findByIdOptional(masterOrderId)).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.save(any())).thenAnswer(invocation -> {
            PaymentAttempt saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                TestFields.set(saved, "id", UUID.randomUUID());
            }
            return saved;
        });

        PaymentDtos.PaymentAttemptDto dto = paymentService.initiatePayment(buyerId, masterOrderId, "IDEMP-KEY-1");

        assertThat(dto).isNotNull();
        assertThat(dto.status()).isEqualTo("PROCESSING");
        assertThat(dto.amount()).isEqualByComparingTo("10000000.00");
        assertThat(dto.gatewayReference()).startsWith("MOCK-");
        assertThat(order.getPaymentStatus()).isEqualTo(MasterOrder.PaymentStatus.PENDING);

        verify(auditService).record(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString());
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Initiate payment is idempotent: same key returns existing attempt, no duplicate")
    void testInitiatePaymentIdempotency() {
        Long buyerId = 1L;
        PaymentAttempt existing = new PaymentAttempt(UUID.randomUUID(), buyerId,
                new BigDecimal("500000.00"), "MOCK-EXISTING", "IDEMP-KEY-1");
        TestFields.set(existing, "id", UUID.randomUUID());
        existing.setStatus(PaymentAttempt.Status.SUCCEEDED);

        when(paymentAttemptRepository.findByIdempotencyKey("IDEMP-KEY-1")).thenReturn(Optional.of(existing));

        PaymentDtos.PaymentAttemptDto dto =
                paymentService.initiatePayment(buyerId, UUID.randomUUID(), "IDEMP-KEY-1");

        assertThat(dto.gatewayReference()).isEqualTo("MOCK-EXISTING");
        assertThat(dto.status()).isEqualTo("SUCCEEDED");
        verify(paymentAttemptRepository, never()).save(any());
        verify(masterOrderRepository, never()).findByIdOptional(any(UUID.class));
    }

    @Test
    @DisplayName("Initiate payment rejects buyer who does not own the master order (IDOR)")
    void testInitiatePaymentOwnershipMismatch() {
        UUID masterOrderId = UUID.randomUUID();
        when(paymentAttemptRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(masterOrderRepository.findByIdOptional(masterOrderId))
                .thenReturn(Optional.of(masterOrder(999L, "1000.00", false)));

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, masterOrderId, "IDEMP-KEY-2"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to this buyer");
    }

    @Test
    @DisplayName("Complete payment: PROCESSING -> SUCCEEDED, master order flips to PAID, outbox PAYMENT_SUCCEEDED")
    void testCompletePaymentHappyPath() {
        UUID attemptId = UUID.randomUUID();
        PaymentAttempt attempt = new PaymentAttempt(UUID.randomUUID(), 1L,
                new BigDecimal("10000000.00"), "MOCK-1", "IDEMP-KEY-3");
        TestFields.set(attempt, "id", attemptId);
        attempt.setStatus(PaymentAttempt.Status.PROCESSING);
        MasterOrder order = masterOrder(1L, "10000000.00", false);

        when(paymentAttemptRepository.findByIdOptional(attemptId)).thenReturn(Optional.of(attempt));
        when(masterOrderRepository.findByIdOptional(attempt.getMasterOrderId())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentDtos.PaymentAttemptDto dto = paymentService.completePayment(attemptId);

        assertThat(dto.status()).isEqualTo("SUCCEEDED");
        assertThat(dto.completedAt()).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(MasterOrder.PaymentStatus.PAID);
        verify(outboxEventRepository).save(any());
        verify(auditService).record(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Complete payment rejects amount mismatch with master order total")
    void testCompletePaymentAmountMismatch() {
        UUID attemptId = UUID.randomUUID();
        PaymentAttempt attempt = new PaymentAttempt(UUID.randomUUID(), 1L,
                new BigDecimal("9000000.00"), "MOCK-2", "IDEMP-KEY-4");
        TestFields.set(attempt, "id", attemptId);
        attempt.setStatus(PaymentAttempt.Status.PROCESSING);

        when(paymentAttemptRepository.findByIdOptional(attemptId)).thenReturn(Optional.of(attempt));
        when(masterOrderRepository.findByIdOptional(attempt.getMasterOrderId()))
                .thenReturn(Optional.of(masterOrder(1L, "10000000.00", false)));

        assertThatThrownBy(() -> paymentService.completePayment(attemptId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not match master order total");
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttempt.Status.PROCESSING);
    }

    @Test
    @DisplayName("Complete payment guards state: SUCCEEDED attempt cannot transition again")
    void testCompletePaymentGuardNonProcessing() {
        UUID attemptId = UUID.randomUUID();
        PaymentAttempt attempt = new PaymentAttempt(UUID.randomUUID(), 1L,
                new BigDecimal("1000.00"), "MOCK-3", "IDEMP-KEY-5");
        TestFields.set(attempt, "id", attemptId);
        attempt.setStatus(PaymentAttempt.Status.SUCCEEDED);

        when(paymentAttemptRepository.findByIdOptional(attemptId)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> paymentService.completePayment(attemptId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be PROCESSING to complete");
    }

    @Test
    @DisplayName("Webhook is idempotent: terminal attempt returns current state without transitions")
    void testWebhookIdempotentOnTerminalStatus() {
        PaymentAttempt succeeded = new PaymentAttempt(UUID.randomUUID(), 1L,
                new BigDecimal("1000.00"), "MOCK-TERM", "IDEMP-KEY-6");
        succeeded.setStatus(PaymentAttempt.Status.SUCCEEDED);

        when(paymentAttemptRepository.findByGatewayReference("MOCK-TERM")).thenReturn(Optional.of(succeeded));

        PaymentDtos.PaymentAttemptDto dto = paymentService.handleWebhook(
                new PaymentDtos.WebhookDto("MOCK-TERM", "SUCCEEDED", null));

        assertThat(dto.status()).isEqualTo("SUCCEEDED");
        verify(paymentAttemptRepository, never()).save(any());
    }
}
