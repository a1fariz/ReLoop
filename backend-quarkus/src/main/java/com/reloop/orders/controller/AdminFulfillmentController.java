package com.reloop.orders.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.orders.dto.FulfillmentOrderDto;
import com.reloop.orders.service.OrderFulfillmentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Admin operations console feed: all fulfillments paged, newest first.
 */
@Path("/api/v1/admin/fulfillments")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminFulfillmentController {
    private final OrderFulfillmentService fulfillmentService;
    private final CorrelationContext correlationContext;

    @Inject
    public AdminFulfillmentController(OrderFulfillmentService fulfillmentService,
                                       CorrelationContext correlationContext) {
        this.fulfillmentService = fulfillmentService;
        this.correlationContext = correlationContext;
    }

    @GET
    public ApiResponse<com.reloop.common.dto.Page<FulfillmentOrderDto>> getAllFulfillments(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = fulfillmentService.getAllFulfillments(Math.max(page, 0), safeSize);
        return ApiResponse.ok(new com.reloop.common.dto.Page<>(
                result.items().stream().map(OrderController::toDto).toList(),
                result.total(), result.page(), result.size()),
                correlationContext.getCorrelationId());
    }
}
