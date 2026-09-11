package com.reloop.catalog.controller;

import com.reloop.catalog.dto.ProductModelDto;
import com.reloop.catalog.service.CatalogService;
import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogController {
    private final CatalogService catalogService;
    private final CorrelationContext correlationContext;

    @Inject
    public CatalogController(CatalogService catalogService, CorrelationContext correlationContext) {
        this.catalogService = catalogService;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/models")
    public ApiResponse<List<ProductModelDto>> getAllModels() {
        return ApiResponse.ok(catalogService.getAllModels(), correlationContext.getCorrelationId());
    }

    @GET
    @Path("/models/{slug}")
    public ApiResponse<ProductModelDto> getModelBySlug(@PathParam("slug") String slug) {
        return ApiResponse.ok(catalogService.getModelBySlug(slug), correlationContext.getCorrelationId());
    }
}
