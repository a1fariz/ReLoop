package com.reloop.warranties.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.warranties.dto.WarrantyDto;
import com.reloop.warranties.service.WarrantyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warranties")
public class WarrantyController {
    private final WarrantyService warrantyService;

    public WarrantyController(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WarrantyDto>>> getMyWarranties(
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletRequest request
    ) {
        Long effectiveUserId = userId != null ? userId : 1L;
        String correlationId = (String) request.getAttribute("X-Correlation-ID");
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.getUserWarranties(effectiveUserId), correlationId));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<ApiResponse<WarrantyDto>> getWarrantyByUnit(
            @PathVariable UUID unitId,
            HttpServletRequest request
    ) {
        String correlationId = (String) request.getAttribute("X-Correlation-ID");
        return ResponseEntity.ok(ApiResponse.ok(warrantyService.getWarrantyByUnitId(unitId), correlationId));
    }
}
