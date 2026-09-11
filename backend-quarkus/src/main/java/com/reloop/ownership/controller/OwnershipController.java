package com.reloop.ownership.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.dto.Page;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.ownership.domain.OwnershipTransfer;
import com.reloop.ownership.dto.OwnershipDtos;
import com.reloop.ownership.service.OwnershipService;
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

import java.util.UUID;

@Path("/api/v1/ownership")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OwnershipController {
    private static final String ROLE_ADMIN = "ADMIN";

    private final OwnershipService ownershipService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public OwnershipController(OwnershipService ownershipService, CurrentUser currentUser,
                               CorrelationContext correlationContext) {
        this.ownershipService = ownershipService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/me")
    public ApiResponse<Page<OwnershipDtos.OwnershipTransferDto>> getMyTransfers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(ownershipService.getMyTransfers(currentUser.id(), Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/units/{unitId}")
    public ApiResponse<OwnershipDtos.OwnershipChainDto> getUnitProvenance(@PathParam("unitId") UUID unitId) {
        return ApiResponse.ok(ownershipService.getUnitProvenance(unitId, currentUser.id(), isAdmin()),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/units/{unitId}/verify")
    @RolesAllowed(ROLE_ADMIN)
    public ApiResponse<OwnershipDtos.OwnershipChainDto> verifyChain(@PathParam("unitId") UUID unitId) {
        return ApiResponse.ok(ownershipService.verifyChain(unitId), correlationContext.getCorrelationId());
    }

    @POST
    @Path("/transfers")
    @RolesAllowed(ROLE_ADMIN)
    public ApiResponse<OwnershipDtos.OwnershipTransferDto> recordTransfer(
            @Valid OwnershipDtos.RecordTransferDto request) {
        OwnershipTransfer.TransferType type = parseTransferType(request.transferType());
        OwnershipDtos.OwnershipTransferDto transfer = ownershipService.recordTransfer(
                request.unitId(), request.fromOwnerId(), request.toOwnerId(), type,
                request.referenceType(), request.referenceId());
        return ApiResponse.ok(transfer, "Ownership transfer recorded", correlationContext.getCorrelationId());
    }

    private boolean isAdmin() {
        return ROLE_ADMIN.equals(currentUser.role());
    }

    private static OwnershipTransfer.TransferType parseTransferType(String raw) {
        try {
            return OwnershipTransfer.TransferType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.reloop.common.exception.BusinessException(
                    "Unknown transfer type: " + raw, "UNKNOWN_TRANSFER_TYPE", 422);
        }
    }
}
