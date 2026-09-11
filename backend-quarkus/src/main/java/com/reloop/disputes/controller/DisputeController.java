package com.reloop.disputes.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.service.DisputeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/disputes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DisputeController {
    private final DisputeService disputeService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public DisputeController(DisputeService disputeService, CurrentUser currentUser, CorrelationContext correlationContext) {
        this.disputeService = disputeService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    public RestResponse<ApiResponse<DisputeDtos.DisputeResponse>> createDispute(@Valid DisputeDtos.CreateDisputeRequest request) {
        DisputeDtos.DisputeResponse response = disputeService.createDispute(currentUser.id(), request);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(response, "Dispute registered", correlationContext.getCorrelationId()));
    }

    @POST
    @Path("/{id}/resolve")
    @RolesAllowed("ADMIN")
    public ApiResponse<DisputeDtos.DisputeResponse> resolveDispute(
            @PathParam("id") UUID id,
            @Valid DisputeDtos.ResolveDisputeRequest request
    ) {
        DisputeDtos.DisputeResponse response = disputeService.resolveDispute(id, currentUser.id(), request);
        return ApiResponse.ok(response, "Dispute resolved with arbitrated split settlement", correlationContext.getCorrelationId());
    }

    @GET
    @Path("/my")
    public ApiResponse<com.reloop.common.dto.Page<DisputeDtos.DisputeResponse>> getMyDisputes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(disputeService.getBuyerDisputes(currentUser.id(), Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/admin/all")
    @RolesAllowed("ADMIN")
    public ApiResponse<com.reloop.common.dto.Page<DisputeDtos.DisputeResponse>> getAllDisputes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(disputeService.getAllDisputes(Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }
}
