package com.reloop.reviews.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.reviews.dto.ReviewDtos;
import com.reloop.reviews.service.ReviewService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReviewController {
    private final ReviewService reviewService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public ReviewController(ReviewService reviewService, CurrentUser currentUser,
                            CorrelationContext correlationContext) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @POST
    public ApiResponse<ReviewDtos.ReviewResponse> submitReview(@Valid ReviewDtos.SubmitReviewRequest request) {
        return ApiResponse.ok(reviewService.submitReview(currentUser.id(), request),
                "Review submitted", correlationContext.getCorrelationId());
    }

    @GET
    @Path("/seller/{sellerId}")
    public ApiResponse<List<ReviewDtos.ReviewResponse>> getSellerReviews(@PathParam("sellerId") Long sellerId) {
        return ApiResponse.ok(reviewService.getSellerReviews(sellerId), correlationContext.getCorrelationId());
    }
}
