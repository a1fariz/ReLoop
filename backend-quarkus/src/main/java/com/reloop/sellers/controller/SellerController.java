package com.reloop.sellers.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.sellers.dto.SellerMetricsResponse;
import com.reloop.sellers.service.SellerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/sellers")
@Produces(MediaType.APPLICATION_JSON)
public class SellerController {
    private final SellerService sellerService;
    private final CorrelationContext correlationContext;

    @Inject
    public SellerController(SellerService sellerService, CorrelationContext correlationContext) {
        this.sellerService = sellerService;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/{id}/metrics")
    public ApiResponse<SellerMetricsResponse> getSellerMetrics(@PathParam("id") Long id) {
        return ApiResponse.ok(sellerService.getSellerMetrics(id), correlationContext.getCorrelationId());
    }
}
