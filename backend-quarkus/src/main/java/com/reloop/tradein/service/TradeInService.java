package com.reloop.tradein.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.tradein.domain.TradeInRequest;
import com.reloop.tradein.dto.TradeInDtos;
import com.reloop.tradein.repository.TradeInRequestRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TradeInService {

    private final TradeInRequestRepository tradeInRequestRepository;
    private final TradeInValuationService valuationService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final UUID instanceCorrelationId = UUID.randomUUID();

    @Inject
    public TradeInService(
            TradeInRequestRepository tradeInRequestRepository,
            TradeInValuationService valuationService,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.tradeInRequestRepository = tradeInRequestRepository;
        this.valuationService = valuationService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates a trade-in request with the server-authoritative offer. The buyer
     * declares the device state; the platform recomputes the offer, ignoring any
     * client-side estimate.
     */
    @Transactional
    public TradeInDtos.TradeInRequestResponse submitTradeIn(Long userId, TradeInDtos.SubmitTradeInRequest request) {
        TradeInValuationService.Condition condition;
        TradeInValuationService.Functionality functionality;
        try {
            condition = TradeInValuationService.Condition.valueOf(request.declaredCondition().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown condition: " + request.declaredCondition(), "INVALID_CONDITION", 400);
        }
        try {
            functionality = TradeInValuationService.Functionality.valueOf(request.declaredFunctionality().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown functionality: " + request.declaredFunctionality(), "INVALID_FUNCTIONALITY", 400);
        }

        BigDecimal estimatedOffer = valuationService.calculateFinalOffer(
                request.msrp(),
                request.annualDepreciationRate(),
                request.releaseDate(),
                condition,
                functionality,
                request.batteryHealthPercentage() != null ? request.batteryHealthPercentage() : 0,
                request.hasCompleteAccessories(),
                request.estimatedRepairCost() != null ? request.estimatedRepairCost() : BigDecimal.ZERO
        );

        TradeInRequest tradeInRequest = new TradeInRequest(
                userId,
                request.productModelId(),
                condition.name(),
                functionality.name(),
                request.batteryHealthPercentage(),
                request.hasCompleteAccessories(),
                estimatedOffer
        );
        tradeInRequest = tradeInRequestRepository.save(tradeInRequest);

        emitTradeInSubmitted(tradeInRequest, userId);
        meterRegistry.counter("reloop.tradein.submitted").increment();
        return toResponse(tradeInRequest);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TradeInDtos.TradeInRequestResponse> getMyRequests(Long userId) {
        return tradeInRequestRepository.findByUserId(userId).stream()
                .map(TradeInService::toResponse)
                .toList();
    }

    private void emitTradeInSubmitted(TradeInRequest request, Long userId) {
        try {
            String payload = objectMapper.writeValueAsString(new TradeInSubmittedPayload(
                    request.getId(), userId, request.getProductModelId(), request.getEstimatedOffer()));
            outboxEventRepository.save(new OutboxEvent(
                    "TRADEIN",
                    request.getId().toString(),
                    "TRADEIN_SUBMITTED",
                    payload,
                    instanceCorrelationId,
                    "TRADEIN_SUBMITTED:" + request.getId()
            ));
        } catch (JsonProcessingException e) {
            // Non-fatal: the request is committed regardless of event serialization
        }
    }

    public static TradeInDtos.TradeInRequestResponse toResponse(TradeInRequest r) {
        return new TradeInDtos.TradeInRequestResponse(
                r.getId(),
                r.getProductModelId(),
                r.getDeclaredCondition(),
                r.getDeclaredFunctionality(),
                r.getDeclaredBatteryHealth(),
                r.isHasCompleteAccessories(),
                r.getEstimatedOffer(),
                r.getStatus().name(),
                r.getCreatedAt()
        );
    }

    record TradeInSubmittedPayload(
            UUID tradeInRequestId,
            Long userId,
            UUID productModelId,
            BigDecimal estimatedOffer
    ) {}
}
