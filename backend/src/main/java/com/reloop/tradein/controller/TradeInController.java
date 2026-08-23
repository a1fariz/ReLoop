package com.reloop.tradein.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.tradein.dto.TradeInCalculationRequest;
import com.reloop.tradein.dto.TradeInOfferResponse;
import com.reloop.tradein.service.TradeInValuationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/trade-in")
public class TradeInController {
    private final TradeInValuationService valuationService;

    public TradeInController(TradeInValuationService valuationService) {
        this.valuationService = valuationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<TradeInOfferResponse>> calculateTradeIn(
            @RequestBody @Valid TradeInCalculationRequest request,
            HttpServletRequest servletRequest
    ) {
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

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
        ).setScale(2, java.math.RoundingMode.HALF_UP);

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

        return ResponseEntity.ok(ApiResponse.ok(response, "Valuation calculated", correlationId));
    }
}
