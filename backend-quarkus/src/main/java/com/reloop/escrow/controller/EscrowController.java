package com.reloop.escrow.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.escrow.dto.EscrowDtos;
import com.reloop.escrow.service.EscrowService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/escrow")
@Produces(MediaType.APPLICATION_JSON)
public class EscrowController {
    private static final String ROLE_ADMIN = "ADMIN";

    private final EscrowService escrowService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public EscrowController(EscrowService escrowService, CurrentUser currentUser, CorrelationContext correlationContext) {
        this.escrowService = escrowService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/{fulfillmentOrderId}")
    public ApiResponse<EscrowDtos.EscrowSummaryDto> getEscrowSummary(
            @PathParam("fulfillmentOrderId") UUID fulfillmentOrderId
    ) {
        return ApiResponse.ok(
                escrowService.getEscrowSummary(fulfillmentOrderId, currentUser.id(), isAdmin()),
                correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{fulfillmentOrderId}/release")
    @RolesAllowed(ROLE_ADMIN)
    public ApiResponse<EscrowDtos.EscrowSummaryDto> releaseEscrow(
            @PathParam("fulfillmentOrderId") UUID fulfillmentOrderId
    ) {
        return ApiResponse.ok(
                escrowService.releaseEscrow(fulfillmentOrderId, currentUser.id()),
                "Escrow force-released and settled", correlationContext.getCorrelationId());
    }

    @GET
    @Path("/stats")
    @RolesAllowed(ROLE_ADMIN)
    public ApiResponse<EscrowDtos.EscrowStatsDto> platformEscrowStats() {
        return ApiResponse.ok(escrowService.platformEscrowStats(), correlationContext.getCorrelationId());
    }

    private boolean isAdmin() {
        return ROLE_ADMIN.equals(currentUser.role());
    }
}
