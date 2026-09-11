package com.reloop.inspections.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.inspections.dto.InspectionDtos;
import com.reloop.inspections.service.InspectionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/inspections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InspectionController {
    private final InspectionService inspectionService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public InspectionController(InspectionService inspectionService, CurrentUser currentUser,
                                CorrelationContext correlationContext) {
        this.inspectionService = inspectionService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<InspectionDtos.InspectionResponse> createInspection(@Valid InspectionDtos.CreateInspectionRequest request) {
        return ApiResponse.ok(inspectionService.createInspection(currentUser.id(), request),
                "Inspection recorded", correlationContext.getCorrelationId());
    }

    @GET
    @Path("/unit/{unitId}")
    public ApiResponse<InspectionDtos.InspectionResponse> getLatestByUnit(@PathParam("unitId") UUID unitId) {
        return ApiResponse.ok(inspectionService.getLatestByUnit(unitId), correlationContext.getCorrelationId());
    }
}
