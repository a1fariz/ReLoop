package com.reloop.refurbishment.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.dto.Page;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.refurbishment.domain.RepairTicket;
import com.reloop.refurbishment.dto.RepairDtos;
import com.reloop.refurbishment.service.RefurbishmentService;
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

@Path("/api/v1/repairs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RepairController {
    private static final String ROLE_ADMIN = "ADMIN";

    private final RefurbishmentService refurbishmentService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public RepairController(RefurbishmentService refurbishmentService, CurrentUser currentUser,
                            CorrelationContext correlationContext) {
        this.refurbishmentService = refurbishmentService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public RestResponse<ApiResponse<RepairDtos.RepairTicketDto>> createTicket(
            @Valid RepairDtos.CreateRepairTicketDto request) {
        RepairDtos.RepairTicketDto ticket = refurbishmentService.createTicket(currentUser.id(), request);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(ticket, "Repair ticket opened", correlationContext.getCorrelationId()));
    }

    @GET
    @Path("/my")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<Page<RepairDtos.RepairTicketDto>> getMyTickets(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(refurbishmentService.listTickets(null, null, currentUser.id(), false,
                Math.max(page, 0), safeSize), correlationContext.getCorrelationId());
    }

    @GET
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<Page<RepairDtos.RepairTicketDto>> listTickets(
            @QueryParam("status") String status,
            @QueryParam("unitId") UUID unitId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        RepairTicket.Status parsedStatus = status != null ? parseStatus(status) : null;
        return ApiResponse.ok(refurbishmentService.listTickets(parsedStatus, unitId, currentUser.id(), isAdmin(),
                Math.max(page, 0), safeSize), correlationContext.getCorrelationId());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<RepairDtos.RepairTicketDto> getTicket(@PathParam("id") UUID id) {
        return ApiResponse.ok(refurbishmentService.getTicket(id, currentUser.id(), isAdmin()),
                correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/start-diagnosis")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<RepairDtos.RepairTicketDto> startDiagnosis(@PathParam("id") UUID id) {
        return ApiResponse.ok(refurbishmentService.startDiagnosis(id, currentUser.id(), isAdmin()),
                "Diagnosis started", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/start-repair")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<RepairDtos.RepairTicketDto> startRepair(
            @PathParam("id") UUID id,
            @QueryParam("estimatedPartsCost") java.math.BigDecimal estimatedPartsCost) {
        return ApiResponse.ok(refurbishmentService.startRepair(id, currentUser.id(), estimatedPartsCost, isAdmin()),
                "Repair in progress", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/submit-qc")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<RepairDtos.RepairTicketDto> submitQc(
            @PathParam("id") UUID id,
            @Valid RepairDtos.SubmitQcDto request) {
        return ApiResponse.ok(refurbishmentService.submitQc(id, currentUser.id(), request, isAdmin()),
                "Submitted for QC", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/complete")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<RepairDtos.RepairTicketDto> complete(
            @PathParam("id") UUID id,
            @Valid RepairDtos.CompleteRepairDto request) {
        return ApiResponse.ok(refurbishmentService.completeTicket(id, currentUser.id(), request, isAdmin()),
                "Repair completed", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/cancel")
    @RolesAllowed({"TECHNICIAN", "ADMIN"})
    public ApiResponse<RepairDtos.RepairTicketDto> cancel(
            @PathParam("id") UUID id,
            @Valid RepairDtos.CancelRepairDto request) {
        return ApiResponse.ok(refurbishmentService.cancelTicket(id, currentUser.id(), request.reason(), isAdmin()),
                "Repair cancelled", correlationContext.getCorrelationId());
    }

    private boolean isAdmin() {
        return ROLE_ADMIN.equals(currentUser.role());
    }

    private static RepairTicket.Status parseStatus(String raw) {
        try {
            return RepairTicket.Status.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.reloop.common.exception.BusinessException(
                    "Unknown repair status: " + raw, "UNKNOWN_REPAIR_STATUS", 422);
        }
    }
}
