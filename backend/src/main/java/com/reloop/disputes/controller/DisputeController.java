package com.reloop.disputes.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.service.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {
    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DisputeDtos.DisputeResponse>> createDispute(
            @Valid @RequestBody DisputeDtos.CreateDisputeRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletRequest servletRequest
    ) {
        Long effectiveUserId = userId != null ? userId : 1L;
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        DisputeDtos.DisputeResponse response = disputeService.createDispute(effectiveUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Dispute registered", correlationId));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<DisputeDtos.DisputeResponse>> resolveDispute(
            @PathVariable UUID id,
            @Valid @RequestBody DisputeDtos.ResolveDisputeRequest request,
            @RequestAttribute(value = "adminId", required = false) Long adminId,
            HttpServletRequest servletRequest
    ) {
        Long effectiveAdminId = adminId != null ? adminId : 999L;
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        DisputeDtos.DisputeResponse response = disputeService.resolveDispute(id, effectiveAdminId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Dispute resolved with arbitrated split settlement", correlationId));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DisputeDtos.DisputeResponse>>> getMyDisputes(
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletRequest servletRequest
    ) {
        Long effectiveUserId = userId != null ? userId : 1L;
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        return ResponseEntity.ok(ApiResponse.ok(disputeService.getBuyerDisputes(effectiveUserId), correlationId));
    }
}
