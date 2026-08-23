package com.reloop.checkout.controller;

import com.reloop.checkout.dto.ReservationResponse;
import com.reloop.checkout.dto.ReserveUnitRequest;
import com.reloop.checkout.service.CheckoutReservationService;
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

    public CheckoutController(CheckoutReservationService reservationService) {
        this.reservationService = reservationService;
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
}
