package com.reloop.returns.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.returns.dto.ReturnDtos;
import com.reloop.returns.service.ReturnService;
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

import java.util.UUID;

@Path("/api/v1/returns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReturnController {
    private final ReturnService returnService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public ReturnController(ReturnService returnService, CurrentUser currentUser, CorrelationContext correlationContext) {
        this.returnService = returnService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    public RestResponse<ApiResponse<ReturnDtos.ReturnResponse>> requestReturn(@Valid ReturnDtos.CreateReturnRequest request) {
        ReturnDtos.ReturnResponse response = returnService.requestReturn(currentUser.id(), request);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(response, "Return requested", correlationContext.getCorrelationId()));
    }

    @GET
    @Path("/my")
    public ApiResponse<com.reloop.common.dto.Page<ReturnDtos.ReturnResponse>> getMyReturns(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(returnService.myReturns(currentUser.id(), Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/{id}")
    public ApiResponse<ReturnDtos.ReturnResponse> getReturn(@PathParam("id") UUID id) {
        boolean isAdmin = "ADMIN".equals(currentUser.role());
        return ApiResponse.ok(returnService.getReturn(id, currentUser.id(), isAdmin), correlationContext.getCorrelationId());
    }

    @GET
    @Path("/admin")
    @RolesAllowed("ADMIN")
    public ApiResponse<com.reloop.common.dto.Page<ReturnDtos.ReturnResponse>> adminReturns(
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(returnService.adminReturns(status, Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/review")
    @RolesAllowed("ADMIN")
    public ApiResponse<ReturnDtos.ReturnResponse> review(
            @PathParam("id") UUID id,
            @Valid ReturnDtos.ReviewReturnRequest request) {
        return ApiResponse.ok(returnService.review(id, currentUser.id(), request),
                "Return reviewed", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/shipment")
    @RolesAllowed("ADMIN")
    public ApiResponse<ReturnDtos.ReturnResponse> recordShipment(
            @PathParam("id") UUID id,
            @Valid ReturnDtos.ShipmentRequest request) {
        return ApiResponse.ok(returnService.recordShipment(id, currentUser.id(), request),
                "Return shipment recorded", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/received")
    @RolesAllowed("ADMIN")
    public ApiResponse<ReturnDtos.ReturnResponse> recordReceived(@PathParam("id") UUID id) {
        return ApiResponse.ok(returnService.recordReceived(id, currentUser.id()),
                "Return package received", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/inspection")
    @RolesAllowed("ADMIN")
    public ApiResponse<ReturnDtos.ReturnResponse> recordInspection(
            @PathParam("id") UUID id,
            @Valid ReturnDtos.InspectionRequest request) {
        return ApiResponse.ok(returnService.recordInspection(id, currentUser.id(), request),
                "Return inspected", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/finalize-refund")
    @RolesAllowed("ADMIN")
    public ApiResponse<ReturnDtos.ReturnResponse> finalizeRefund(@PathParam("id") UUID id) {
        return ApiResponse.ok(returnService.finalizeRefund(id, currentUser.id()),
                "Return refunded; escrow journal posted", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/close")
    @RolesAllowed("ADMIN")
    public ApiResponse<ReturnDtos.ReturnResponse> close(@PathParam("id") UUID id) {
        return ApiResponse.ok(returnService.close(id, currentUser.id()),
                "Return closed", correlationContext.getCorrelationId());
    }
}
