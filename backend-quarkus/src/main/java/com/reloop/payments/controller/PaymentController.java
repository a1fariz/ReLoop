package com.reloop.payments.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.payments.dto.PaymentDtos;
import com.reloop.payments.service.PaymentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentController {
    private static final String ROLE_ADMIN = "ADMIN";

    private final PaymentService paymentService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public PaymentController(PaymentService paymentService, CurrentUser currentUser, CorrelationContext correlationContext) {
        this.paymentService = paymentService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    @Path("/initiate")
    public RestResponse<ApiResponse<PaymentDtos.PaymentAttemptDto>> initiatePayment(
            @Valid PaymentDtos.InitiatePaymentDto request,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        PaymentDtos.PaymentAttemptDto response =
                paymentService.initiatePayment(currentUser.id(), request.masterOrderId(), idempotencyKey);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(response, "Payment attempt initiated with mock gateway",
                        correlationContext.getCorrelationId()));
    }

    @POST
    @Path("/{attemptId}/complete")
    public ApiResponse<PaymentDtos.PaymentAttemptDto> completePayment(@PathParam("attemptId") UUID attemptId) {
        PaymentDtos.PaymentAttemptDto response = paymentService.completePayment(attemptId);
        return ApiResponse.ok(response, "Payment succeeded (mock gateway simulation)",
                correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{attemptId}/fail")
    public ApiResponse<PaymentDtos.PaymentAttemptDto> failPayment(
            @PathParam("attemptId") UUID attemptId,
            @QueryParam("reason") @DefaultValue("Simulated failure") String reason
    ) {
        PaymentDtos.PaymentAttemptDto response = paymentService.failPayment(attemptId, reason);
        return ApiResponse.ok(response, "Payment failed (mock gateway simulation)",
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/attempts/{attemptId}")
    public ApiResponse<PaymentDtos.PaymentAttemptDto> getAttempt(@PathParam("attemptId") UUID attemptId) {
        return ApiResponse.ok(
                paymentService.getAttempt(attemptId, currentUser.id(), isAdmin()),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/my")
    public ApiResponse<com.reloop.common.dto.Page<PaymentDtos.PaymentAttemptDto>> getMyAttempts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(paymentService.getBuyerAttempts(currentUser.id(), Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/admin")
    @RolesAllowed(ROLE_ADMIN)
    public ApiResponse<com.reloop.common.dto.Page<PaymentDtos.PaymentAttemptDto>> getAttemptsForBuyer(
            @QueryParam("buyerId") Long buyerId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long target = buyerId != null ? buyerId : currentUser.id();
        return ApiResponse.ok(paymentService.getAllAttempts(target, Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    /**
     * Mock gateway webhook. Public path via quarkus.http.auth.permission.webhook
     * (exact-match permission wins over the /api/* authenticated rule); the
     * handler is idempotent on terminal statuses.
     */
    @POST
    @Path("/webhook")
    public ApiResponse<PaymentDtos.PaymentAttemptDto> handleWebhook(@Valid PaymentDtos.WebhookDto request) {
        PaymentDtos.PaymentAttemptDto response = paymentService.handleWebhook(request);
        return ApiResponse.ok(response, "Webhook processed", correlationContext.getCorrelationId());
    }

    private boolean isAdmin() {
        return ROLE_ADMIN.equals(currentUser.role());
    }
}
