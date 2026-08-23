package com.reloop.catalog.controller;

import com.reloop.catalog.dto.ProductModelDto;
import com.reloop.catalog.service.CatalogService;
import com.reloop.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<ProductModelDto>>> getAllModels(HttpServletRequest request) {
        String correlationId = (String) request.getAttribute("X-Correlation-ID");
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getAllModels(), correlationId));
    }

    @GetMapping("/models/{slug}")
    public ResponseEntity<ApiResponse<ProductModelDto>> getModelBySlug(
            @PathVariable String slug,
            HttpServletRequest request
    ) {
        String correlationId = (String) request.getAttribute("X-Correlation-ID");
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getModelBySlug(slug), correlationId));
    }
}
