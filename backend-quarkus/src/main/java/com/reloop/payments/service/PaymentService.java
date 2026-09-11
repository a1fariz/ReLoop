package com.reloop.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.payments.domain.PaymentAttempt;
import com.reloop.payments.dto.PaymentDtos;
import com.reloop.payments.repository.PaymentAttemptRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment attempts lifecycle over the mock gateway. Payment success itself does
 * NOT duplicate checkout saga semantics: escrow holding + order PAID remain the
 * checkout flow's job. Here success only flips the attempt, marks the master
 * order PAID (mirroring how the orders module mutates MasterOrder via its
 * repository) and emits a PAYMENT_SUCCEEDED outbox event for downstream
 * consumers.
 */
@ApplicationScoped
public class PaymentService {
    private static final Logger log = Logger.getLogger(PaymentService.class);

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final MasterOrderRepository masterOrderRepository;
    private final AuditService auditService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public PaymentService(
            PaymentAttemptRepository paymentAttemptRepository,
            MasterOrderRepository masterOrderRepository,
            AuditService auditService,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.masterOrderRepository = masterOrderRepository;
        this.auditService = auditService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public PaymentDtos.PaymentAttemptDto initiatePayment(Long buyerId, UUID masterOrderId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        // Idempotent: same key returns the existing attempt, no duplicate charge
        Optional<PaymentAttempt> existing = paymentAttemptRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(masterOrderId)
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));
        if (!masterOrder.getBuyerId().equals(buyerId)) {
            throw new BusinessException("Master order does not belong to this buyer", "PAYMENT_FORBIDDEN", 403);
        }
        if (masterOrder.getPaymentStatus() == MasterOrder.PaymentStatus.PAID) {
            throw new BusinessException("Master order is already paid", "ORDER_ALREADY_PAID", 409);
        }

        PaymentAttempt attempt = new PaymentAttempt(
                masterOrderId,
                buyerId,
                masterOrder.getTotalAmount(),
                "MOCK-" + UUID.randomUUID(),
                idempotencyKey
        );
        // Mock gateway: INITIATED -> PROCESSING immediately
        attempt.setStatus(PaymentAttempt.Status.PROCESSING);
        attempt = paymentAttemptRepository.save(attempt);

        auditService.record("PaymentAttempt", attempt.getId().toString(), "PAYMENT_INITIATED", buyerId,
                null, PaymentAttempt.Status.INITIATED.name(), PaymentAttempt.Status.PROCESSING.name());
        emit("PAYMENT_INITIATED", "PAYMENT_INITIATED:" + attempt.getId(), payload(
                attempt.getId(), attempt.getMasterOrderId(), attempt.getBuyerId(), attempt.getAmount(), null));
        return toDto(attempt);
    }

    @Transactional
    public PaymentDtos.PaymentAttemptDto completePayment(UUID attemptId) {
        PaymentAttempt attempt = load(attemptId);
        if (attempt.getStatus() != PaymentAttempt.Status.PROCESSING) {
            throw new BusinessException("Payment attempt must be PROCESSING to complete (current: "
                    + attempt.getStatus() + ")", "INVALID_PAYMENT_STATE", 409);
        }

        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(attempt.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));
        if (masterOrder.getTotalAmount().compareTo(attempt.getAmount()) != 0) {
            throw new BusinessException(
                    String.format("Attempt amount %s does not match master order total %s",
                            attempt.getAmount(), masterOrder.getTotalAmount()),
                    "PAYMENT_AMOUNT_MISMATCH", 409);
        }

        attempt.setStatus(PaymentAttempt.Status.SUCCEEDED);
        attempt.setCompletedAt(Instant.now());
        attempt = paymentAttemptRepository.save(attempt);

        if (masterOrder.getPaymentStatus() != MasterOrder.PaymentStatus.PAID) {
            masterOrder.setPaymentStatus(MasterOrder.PaymentStatus.PAID);
            masterOrderRepository.save(masterOrder);
        }

        auditService.record("PaymentAttempt", attempt.getId().toString(), "PAYMENT_SUCCEEDED", attempt.getBuyerId(),
                null, PaymentAttempt.Status.PROCESSING.name(), PaymentAttempt.Status.SUCCEEDED.name());
        emit("PAYMENT_SUCCEEDED", "PAYMENT_SUCCEEDED:" + attempt.getId(), payload(
                attempt.getId(), attempt.getMasterOrderId(), attempt.getBuyerId(), attempt.getAmount(), null));
        return toDto(attempt);
    }

    @Transactional
    public PaymentDtos.PaymentAttemptDto failPayment(UUID attemptId, String reason) {
        PaymentAttempt attempt = load(attemptId);
        if (attempt.getStatus() != PaymentAttempt.Status.PROCESSING) {
            throw new BusinessException("Payment attempt must be PROCESSING to fail (current: "
                    + attempt.getStatus() + ")", "INVALID_PAYMENT_STATE", 409);
        }

        attempt.setStatus(PaymentAttempt.Status.FAILED);
        attempt.setFailureReason(reason);
        attempt.setCompletedAt(Instant.now());
        attempt = paymentAttemptRepository.save(attempt);

        auditService.record("PaymentAttempt", attempt.getId().toString(), "PAYMENT_FAILED", attempt.getBuyerId(),
                null, PaymentAttempt.Status.PROCESSING.name(), PaymentAttempt.Status.FAILED.name());
        emit("PAYMENT_FAILED", "PAYMENT_FAILED:" + attempt.getId(), payload(
                attempt.getId(), attempt.getMasterOrderId(), attempt.getBuyerId(), attempt.getAmount(), reason));
        return toDto(attempt);
    }

    /** Gateway webhook: idempotent — terminal attempts return current state. */
    @Transactional
    public PaymentDtos.PaymentAttemptDto handleWebhook(PaymentDtos.WebhookDto webhook) {
        PaymentAttempt attempt = paymentAttemptRepository.findByGatewayReference(webhook.gatewayReference())
                .orElseThrow(() -> new BusinessException("Unknown gateway reference", "PAYMENT_ATTEMPT_NOT_FOUND", 404));

        if (attempt.getStatus() == PaymentAttempt.Status.SUCCEEDED
                || attempt.getStatus() == PaymentAttempt.Status.FAILED
                || attempt.getStatus() == PaymentAttempt.Status.EXPIRED) {
            return toDto(attempt); // already terminal, idempotent replay
        }
        if (attempt.getStatus() != PaymentAttempt.Status.PROCESSING) {
            throw new BusinessException("Payment attempt must be PROCESSING to transition (current: "
                    + attempt.getStatus() + ")", "INVALID_PAYMENT_STATE", 409);
        }

        return "SUCCEEDED".equals(webhook.status())
                ? completePayment(attempt.getId())
                : failPayment(attempt.getId(), webhook.failureReason());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PaymentDtos.PaymentAttemptDto getAttempt(UUID attemptId, Long userId, boolean admin) {
        PaymentAttempt attempt = load(attemptId);
        if (!admin && !attempt.getBuyerId().equals(userId)) {
            // IDOR guard: buyers only see attempts for their own master orders
            throw new BusinessException("Payment attempt does not belong to this buyer", "PAYMENT_FORBIDDEN", 403);
        }
        return toDto(attempt);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<PaymentDtos.PaymentAttemptDto> getBuyerAttempts(Long buyerId, int page, int size) {
        var query = paymentAttemptRepository.find("buyerId = ?1",
                io.quarkus.panache.common.Sort.descending("createdAt"), buyerId);
        long total = query.count();
        List<PaymentAttempt> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(PaymentService::toDto).toList(), total, page, size);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<PaymentDtos.PaymentAttemptDto> getAllAttempts(Long buyerId, int page, int size) {
        var query = paymentAttemptRepository.find("buyerId = ?1",
                io.quarkus.panache.common.Sort.descending("createdAt"), buyerId);
        long total = query.count();
        List<PaymentAttempt> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(PaymentService::toDto).toList(), total, page, size);
    }

    private PaymentAttempt load(UUID attemptId) {
        return paymentAttemptRepository.findByIdOptional(attemptId)
                .orElseThrow(() -> new BusinessException("Payment attempt not found", "PAYMENT_ATTEMPT_NOT_FOUND", 404));
    }

    private record PaymentPayload(UUID attemptId, UUID masterOrderId, Long buyerId,
                                  BigDecimal amount, String failureReason) {}

    private String payload(UUID attemptId, UUID masterOrderId, Long buyerId, BigDecimal amount, String failureReason) {
        try {
            return objectMapper.writeValueAsString(
                    new PaymentPayload(attemptId, masterOrderId, buyerId, amount, failureReason));
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize payment payload", "PAYMENT_SERIALIZATION_ERROR", 500);
        }
    }

    private void emit(String eventType, String key, String payloadJson) {
        try {
            // Payload is pre-serialized (possibly blank-safe); outbox stores it as jsonb
            outboxEventRepository.save(new OutboxEvent(
                    "PAYMENT",
                    key.substring(key.indexOf(':') + 1),
                    eventType,
                    payloadJson,
                    instanceCorrelationId,
                    key
            ));
        } catch (RuntimeException e) {
            // Non-fatal: the financial state is committed regardless of event emission
            log.errorf(e, "Failed to persist %s outbox event", eventType);
        }
    }

    public static PaymentDtos.PaymentAttemptDto toDto(PaymentAttempt a) {
        return new PaymentDtos.PaymentAttemptDto(
                a.getId(),
                a.getMasterOrderId(),
                a.getBuyerId(),
                a.getAmount(),
                a.getCurrency(),
                a.getGateway(),
                a.getGatewayReference(),
                a.getStatus().name(),
                a.getFailureReason(),
                a.getInitiatedAt(),
                a.getCompletedAt(),
                a.getCreatedAt()
        );
    }
}
