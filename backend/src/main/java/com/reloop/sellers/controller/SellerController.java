package com.reloop.sellers.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.sellers.dto.SellerMetricsResponse;
import com.reloop.sellers.service.SellerReputationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/sellers")
public class SellerController {
    private final SellerReputationService reputationService;

    public SellerController(SellerReputationService reputationService) {
        this.reputationService = reputationService;
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<ApiResponse<SellerMetricsResponse>> getSellerMetrics(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String correlationId = (String) request.getAttribute("X-Correlation-ID");

        // Example calculated seller metrics using Bayesian smoothing
        BigDecimal reputation = reputationService.calculateReputationScore(4.9, 120, 0.01, 0.00, 0.99, 0);

        SellerMetricsResponse response = new SellerMetricsResponse(
                id,
                "Certified Refurbish Hub",
                "certified-hub",
                reputation,
                new BigDecimal("0.010"),
                new BigDecimal("0.000"),
                new BigDecimal("0.990"),
                120,
                0,
                "VERIFIED"
        );

        return ResponseEntity.ok(ApiResponse.ok(response, correlationId));
    }
}
