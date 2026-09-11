package com.reloop.orders.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.dto.FulfillmentOrderDto;
import com.reloop.orders.dto.MasterOrderDto;
import com.reloop.orders.dto.ShipRequest;
import com.reloop.orders.service.OrderFulfillmentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderController {
    private final OrderFulfillmentService fulfillmentService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public OrderController(OrderFulfillmentService fulfillmentService, CurrentUser currentUser,
                           CorrelationContext correlationContext) {
        this.fulfillmentService = fulfillmentService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/my")
    public ApiResponse<com.reloop.common.dto.Page<MasterOrderDto>> getMyOrders(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = fulfillmentService.getBuyerOrders(currentUser.id(), Math.max(page, 0), safeSize);
        return ApiResponse.ok(new com.reloop.common.dto.Page<>(
                result.items().stream().map(OrderController::toDto).toList(),
                result.total(), result.page(), result.size()),
                correlationContext.getCorrelationId());
    }

    @GET
    @Path("/seller/my")
    public ApiResponse<com.reloop.common.dto.Page<FulfillmentOrderDto>> getMyFulfillments(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = fulfillmentService.getSellerFulfillments(currentUser.id(), Math.max(page, 0), safeSize);
        return ApiResponse.ok(new com.reloop.common.dto.Page<>(
                result.items().stream().map(OrderController::toDto).toList(),
                result.total(), result.page(), result.size()),
                correlationContext.getCorrelationId());
    }

    @POST
    @Path("/fulfillment/{id}/ship")
    @Consumes(MediaType.APPLICATION_JSON)
    public ApiResponse<FulfillmentOrderDto> ship(@PathParam("id") java.util.UUID id, @Valid ShipRequest request) {
        FulfillmentOrder fulfillment = fulfillmentService.ship(id, currentUser.id(),
                request.trackingNumber(), request.courierName());
        return ApiResponse.ok(toDto(fulfillment), "Fulfillment marked as shipped", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/fulfillment/{id}/deliver")
    @RolesAllowed("ADMIN")
    public ApiResponse<FulfillmentOrderDto> deliver(@PathParam("id") java.util.UUID id) {
        return ApiResponse.ok(toDto(fulfillmentService.deliver(id)),
                "Fulfillment marked as delivered", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/fulfillment/{id}/complete")
    @RolesAllowed("ADMIN")
    public ApiResponse<FulfillmentOrderDto> complete(@PathParam("id") java.util.UUID id) {
        return ApiResponse.ok(toDto(fulfillmentService.complete(id)),
                "Fulfillment completed; escrow settled and warranty issued", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/fulfillment/{id}/payout")
    @RolesAllowed("ADMIN")
    public ApiResponse<FulfillmentOrderDto> disbursePayout(@PathParam("id") java.util.UUID id) {
        return ApiResponse.ok(toDto(fulfillmentService.disbursePayout(id)),
                "Seller payout disbursed", correlationContext.getCorrelationId());
    }

    public static FulfillmentOrderDto toDto(FulfillmentOrder f) {
        return new FulfillmentOrderDto(
                f.getId(),
                f.getMasterOrderId(),
                f.getSellerId(),
                f.getUnitId(),
                f.getSubtotalAmount(),
                f.getPlatformFeeAmount(),
                f.getSellerNetAmount(),
                f.getFulfillmentStatus().name(),
                f.getEscrowStatus().name(),
                f.getTrackingNumber(),
                f.getCourierName(),
                f.getShippedAt(),
                f.getDeliveredAt(),
                f.getCreatedAt()
        );
    }

    public static MasterOrderDto toDto(MasterOrder m) {
        return new MasterOrderDto(
                m.getId(),
                m.getOrderNumber(),
                m.getBuyerId(),
                m.getTotalAmount(),
                m.getPaymentStatus().name(),
                m.getCreatedAt()
        );
    }
}
