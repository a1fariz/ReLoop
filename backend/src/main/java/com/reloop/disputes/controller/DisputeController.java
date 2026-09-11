package com.reloop.disputes.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.service.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Long userId,
            HttpServletRequest servletRequest
    ) {
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        DisputeDtos.DisputeResponse response = disputeService.createDispute(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Dispute registered", correlationId));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<DisputeDtos.DisputeResponse>> resolveDispute(
            @PathVariable UUID id,
            @Valid @RequestBody DisputeDtos.ResolveDisputeRequest request,
            @AuthenticationPrincipal Long adminId,
            HttpServletRequest servletRequest
    ) {
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        DisputeDtos.DisputeResponse response = disputeService.resolveDispute(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Dispute resolved with arbitrated split settlement", correlationId));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DisputeDtos.DisputeResponse>>> getMyDisputes(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest servletRequest
    ) {
        String correlationId = (String) servletRequest.getAttribute("X-Correlation-ID");

        return ResponseEntity.ok(ApiResponse.ok(disputeService.getBuyerDisputes(userId), correlationId));
    }
}
