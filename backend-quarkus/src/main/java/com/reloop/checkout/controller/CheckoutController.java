package com.reloop.checkout.controller;

import com.reloop.checkout.dto.ConfirmPaymentRequest;
import com.reloop.checkout.dto.OrderConfirmationResponse;
import com.reloop.checkout.dto.ReservationResponse;
import com.reloop.checkout.dto.ReserveUnitRequest;
import com.reloop.checkout.service.CheckoutReservationService;
import com.reloop.checkout.service.CheckoutSagaService;
import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/checkout")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckoutController {
    private final CheckoutReservationService reservationService;
    private final CheckoutSagaService checkoutSagaService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public CheckoutController(
            CheckoutReservationService reservationService,
            CheckoutSagaService checkoutSagaService,
            CurrentUser currentUser,
            CorrelationContext correlationContext
    ) {
        this.reservationService = reservationService;
        this.checkoutSagaService = checkoutSagaService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    @Path("/reserve")
    public RestResponse<ApiResponse<ReservationResponse>> createReservation(ReserveUnitRequest request) {
        ReservationResponse response = reservationService.createReservationLease(currentUser.id(), request);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(response, "15-minute checkout lease acquired", correlationContext.getCorrelationId()));
    }

    @POST
    @Path("/confirm-payment")
    public ApiResponse<OrderConfirmationResponse> confirmPayment(
            ConfirmPaymentRequest request,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        OrderConfirmationResponse response =
                checkoutSagaService.processPaymentAndSettleOrder(currentUser.id(), request, idempotencyKey, currentUser.email());
        return ApiResponse.ok(response, "Payment confirmed and escrow held", correlationContext.getCorrelationId());
    }
}
