package com.reloop.tradein.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.tradein.dto.TradeInCalculationRequest;
import com.reloop.tradein.dto.TradeInOfferResponse;
import com.reloop.tradein.service.TradeInValuationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Path("/api/v1/trade-in")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TradeInController {
    private final TradeInValuationService valuationService;
    private final CorrelationContext correlationContext;

    @Inject
    public TradeInController(TradeInValuationService valuationService, CorrelationContext correlationContext) {
        this.valuationService = valuationService;
        this.correlationContext = correlationContext;
    }

    @POST
    @Path("/calculate")
    public ApiResponse<TradeInOfferResponse> calculateTradeIn(@Valid TradeInCalculationRequest request) {
        TradeInValuationService.Condition condition;
        TradeInValuationService.Functionality functionality;
        try {
            condition = TradeInValuationService.Condition.valueOf(request.condition().toUpperCase());
        } catch (Exception e) {
            condition = TradeInValuationService.Condition.EXCELLENT;
        }

        try {
            functionality = TradeInValuationService.Functionality.valueOf(request.functionality().toUpperCase());
        } catch (Exception e) {
            functionality = TradeInValuationService.Functionality.FULLY_FUNCTIONAL;
        }

        BigDecimal baseValue = valuationService.calculateBaseValue(
                request.msrp(),
                request.annualDepreciationRate(),
                request.releaseDate(),
                LocalDate.now()
        ).setScale(2, RoundingMode.HALF_UP);

        BigDecimal batteryMultiplier = valuationService.getBatteryMultiplier(request.batteryHealthPercentage());
        BigDecimal accessoriesMultiplier = request.hasCompleteAccessories() ? new BigDecimal("1.03") : BigDecimal.ONE;

        BigDecimal finalOffer = valuationService.calculateFinalOffer(
                request.msrp(),
                request.annualDepreciationRate(),
                request.releaseDate(),
                condition,
                functionality,
                request.batteryHealthPercentage(),
                request.hasCompleteAccessories(),
                request.estimatedRepairCost() != null ? request.estimatedRepairCost() : BigDecimal.ZERO
        );

        TradeInOfferResponse response = new TradeInOfferResponse(
                finalOffer,
                baseValue,
                condition.getMultiplier(),
                batteryMultiplier,
                accessoriesMultiplier,
                new BigDecimal("0.15")
        );

        return ApiResponse.ok(response, "Valuation calculated", correlationContext.getCorrelationId());
    }
}
