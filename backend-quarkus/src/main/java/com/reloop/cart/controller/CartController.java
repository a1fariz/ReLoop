package com.reloop.cart.controller;

import com.reloop.cart.dto.CartDtos;
import com.reloop.cart.service.CartService;
import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

@Path("/api/v1/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartController {
    private final CartService cartService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public CartController(CartService cartService, CurrentUser currentUser, CorrelationContext correlationContext) {
        this.cartService = cartService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    public ApiResponse<CartDtos.CartDto> getCart() {
        return ApiResponse.ok(cartService.getCart(currentUser.id()), correlationContext.getCorrelationId());
    }

    @POST
    @Path("/items")
    public RestResponse<ApiResponse<CartDtos.CartItemDto>> addItem(@Valid CartDtos.AddCartItemDto request) {
        int quantity = request.quantity() != null ? request.quantity() : 1;
        CartDtos.CartItemDto item = cartService.addItem(currentUser.id(), request.listingId(), quantity);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(item, "Added to cart", correlationContext.getCorrelationId()));
    }

    @DELETE
    @Path("/items/{listingId}")
    public RestResponse<ApiResponse<String>> removeItem(@PathParam("listingId") UUID listingId) {
        cartService.removeItem(currentUser.id(), listingId);
        return RestResponse.ok(ApiResponse.ok("removed", "Item removed from cart", correlationContext.getCorrelationId()));
    }

    @DELETE
    public ApiResponse<String> clearCart() {
        cartService.clearCart(currentUser.id());
        return ApiResponse.ok("cleared", "Cart cleared", correlationContext.getCorrelationId());
    }
}
