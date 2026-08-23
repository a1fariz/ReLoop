package com.reloop.listings.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.listings.dto.ListingDto;
import com.reloop.listings.service.ListingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {
    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingDto>>> getActiveListings(HttpServletRequest request) {
        String correlationId = (String) request.getAttribute("X-Correlation-ID");
        return ResponseEntity.ok(ApiResponse.ok(listingService.getActiveListings(), correlationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingDto>> getListingById(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        String correlationId = (String) request.getAttribute("X-Correlation-ID");
        return ResponseEntity.ok(ApiResponse.ok(listingService.getListingById(id), correlationId));
    }
}
