package com.reloop.listings.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.dto.Page;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.listings.dto.CreateListingRequest;
import com.reloop.listings.dto.ListingDto;
import com.reloop.listings.dto.UpdateListingRequest;
import com.reloop.listings.service.ListingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/listings")
@Produces(MediaType.APPLICATION_JSON)
public class ListingController {
    private static final int MAX_PAGE_SIZE = 100;

    private final ListingService listingService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public ListingController(ListingService listingService, CurrentUser currentUser,
                             CorrelationContext correlationContext) {
        this.listingService = listingService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    public ApiResponse<List<ListingDto>> getActiveListings() {
        return ApiResponse.ok(listingService.getActiveListings(), correlationContext.getCorrelationId());
    }

    /** Paged marketplace search: minPrice/maxPrice/grade filters + price/newest sorting. */
    @GET
    @Path("/search")
    public ApiResponse<Page<ListingDto>> search(
            @QueryParam("q") String query,
            @QueryParam("minPrice") BigDecimal minPrice,
            @QueryParam("maxPrice") BigDecimal maxPrice,
            @QueryParam("grade") String grade,
            @QueryParam("sort") @DefaultValue("newest") String sort,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Page<ListingDto> result = listingService.searchListings(query, minPrice, maxPrice, grade, sort, safePage, safeSize);
        return ApiResponse.ok(result, correlationContext.getCorrelationId());
    }

    @GET
    @Path("/{id}")
    public ApiResponse<ListingDto> getListingById(@PathParam("id") UUID id) {
        return ApiResponse.ok(listingService.getListingById(id), correlationContext.getCorrelationId());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ApiResponse<ListingDto> createListing(@Valid CreateListingRequest request) {
        return ApiResponse.ok(listingService.createListing(currentUser.id(), request),
                "Listing created", correlationContext.getCorrelationId());
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ApiResponse<ListingDto> updateListing(@PathParam("id") UUID id, @Valid UpdateListingRequest request) {
        return ApiResponse.ok(listingService.updateListing(id, currentUser.id(), request),
                "Listing updated", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/pause")
    public ApiResponse<ListingDto> pauseListing(@PathParam("id") UUID id) {
        return ApiResponse.ok(listingService.pauseListing(id, currentUser.id()),
                "Listing paused", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/resume")
    public ApiResponse<ListingDto> resumeListing(@PathParam("id") UUID id) {
        return ApiResponse.ok(listingService.resumeListing(id, currentUser.id()),
                "Listing resumed", correlationContext.getCorrelationId());
    }
}
