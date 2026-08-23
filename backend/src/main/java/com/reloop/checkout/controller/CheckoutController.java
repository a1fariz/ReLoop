package com.reloop.checkout.controller;

import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.dto.ReservationResponse;
import com.reloop.checkout.dto.ReserveUnitRequest;
import com.reloop.checkout.service.CheckoutReservationService;
import com.reloop.checkout.service.CheckoutSagaService;
import com.reloop.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReserveUnitRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletRequest servletRequest
    ) {
        Long effectiveUserId = userId != null ? userId : 1L; // Fallback mock for demo auth context
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        ReservationResponse response = reservationService.createReservationLease(effectiveUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "15-minute checkout lease acquired", correlationId));
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<ApiResponse<OrderConfirmationResponse>> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletRequest servletRequest
    ) {
        Long effectiveUserId = userId != null ? userId : 1L;
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        OrderConfirmationResponse response = checkoutSagaService.processPaymentAndSettleOrder(effectiveUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Payment confirmed and escrow held", correlationId));
    }
}
