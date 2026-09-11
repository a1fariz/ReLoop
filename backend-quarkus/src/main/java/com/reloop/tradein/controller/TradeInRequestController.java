package com.reloop.tradein.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.tradein.dto.TradeInDtos;
import com.reloop.tradein.service.TradeInService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/trade-in")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TradeInRequestController {
    private final TradeInService tradeInService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public TradeInRequestController(TradeInService tradeInService, CurrentUser currentUser,
                                    CorrelationContext correlationContext) {
        this.tradeInService = tradeInService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    @Path("/requests")
    public ApiResponse<TradeInDtos.TradeInRequestResponse> submitTradeIn(@Valid TradeInDtos.SubmitTradeInRequest request) {
        return ApiResponse.ok(tradeInService.submitTradeIn(currentUser.id(), request),
                "Trade-in request submitted", correlationContext.getCorrelationId());
    }

    @GET
    @Path("/requests/my")
    public ApiResponse<List<TradeInDtos.TradeInRequestResponse>> getMyRequests() {
        return ApiResponse.ok(tradeInService.getMyRequests(currentUser.id()), correlationContext.getCorrelationId());
    }
}
