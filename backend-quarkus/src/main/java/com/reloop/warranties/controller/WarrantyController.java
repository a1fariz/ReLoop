package com.reloop.warranties.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.warranties.dto.WarrantyDto;
import com.reloop.warranties.service.WarrantyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/warranties")
@Produces(MediaType.APPLICATION_JSON)
public class WarrantyController {
    private final WarrantyService warrantyService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public WarrantyController(WarrantyService warrantyService, CurrentUser currentUser, CorrelationContext correlationContext) {
        this.warrantyService = warrantyService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/my")
    public ApiResponse<com.reloop.common.dto.Page<WarrantyDto>> getMyWarranties(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(warrantyService.getUserWarrantiesPaged(currentUser.id(), Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/unit/{unitId}")
    public ApiResponse<WarrantyDto> getWarrantyByUnit(@PathParam("unitId") UUID unitId) {
        return ApiResponse.ok(warrantyService.getWarrantyByUnitId(unitId), correlationContext.getCorrelationId());
    }
}
