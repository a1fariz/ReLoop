package com.reloop.checkout.controller;

import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.dto.ReservationResponse;
import com.reloop.checkout.dto.ReserveUnitRequest;
import com.reloop.checkout.service.CheckoutReservationService;
import com.reloop.checkout.service.CheckoutSagaService;
import com.reloop.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {
    private final CheckoutReservationService reservationService;
    private final CheckoutSagaService checkoutSagaService;

    public CheckoutController(
            CheckoutReservationService reservationService,
            CheckoutSagaService checkoutSagaService
    ) {
        this.reservationService = reservationService;
        this.checkoutSagaService = checkoutSagaService;
    }

    @PostMapping(value = "/reserve", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) ReserveUnitRequest request,
            HttpServletRequest servletRequest
    ) {
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        ReservationResponse response = reservationService.createReservationLease(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "15-minute checkout lease acquired", correlationId));
    }

    @PostMapping(value = "/confirm-payment", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<OrderConfirmationResponse>> confirmPayment(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) ConfirmPaymentRequest request,
            HttpServletRequest servletRequest
    ) {
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");
        String idempotencyKey = servletRequest.getHeader("Idempotency-Key");

        OrderConfirmationResponse response = checkoutSagaService.processPaymentAndSettleOrder(userId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(response, "Payment confirmed and escrow held", correlationId));
    }
}
