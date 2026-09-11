package com.reloop.reviews.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.reviews.domain.Review;
import com.reloop.reviews.dto.ReviewDtos;
import com.reloop.reviews.repository.ReviewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final MasterOrderRepository masterOrderRepository;

    @Inject
    public ReviewService(
            ReviewRepository reviewRepository,
            FulfillmentOrderRepository fulfillmentOrderRepository,
            MasterOrderRepository masterOrderRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.masterOrderRepository = masterOrderRepository;
    }

    /**
     * Only the buyer of a COMPLETED fulfillment can review it, exactly once.
     */
    @Transactional
    public ReviewDtos.ReviewResponse submitReview(Long buyerId, ReviewDtos.SubmitReviewRequest request) {
        FulfillmentOrder fulfillment = fulfillmentOrderRepository.findByIdOptional(request.fulfillmentOrderId())
                .orElseThrow(() -> new BusinessException("Fulfillment order not found", "FULFILLMENT_NOT_FOUND", 404));

        MasterOrder masterOrder = masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId())
                .orElseThrow(() -> new BusinessException("Master order not found", "MASTER_ORDER_NOT_FOUND", 404));
        if (!masterOrder.getBuyerId().equals(buyerId)) {
            throw new BusinessException("Only the buyer of this order can review it", "REVIEW_FORBIDDEN", 403);
        }
        if (fulfillment.getFulfillmentStatus() != FulfillmentOrder.FulfillmentStatus.COMPLETED) {
            throw new BusinessException("Only completed fulfillments can be reviewed", "ORDER_NOT_COMPLETED", 409);
        }
        if (reviewRepository.findByFulfillmentOrderId(request.fulfillmentOrderId()).isPresent()) {
            throw new BusinessException("This order has already been reviewed", "ALREADY_REVIEWED", 409);
        }

        Review review = new Review(
                fulfillment.getId(),
                fulfillment.getUnitId(),
                fulfillment.getSellerId(),
                buyerId,
                request.rating(),
                request.comment()
        );
        review = reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ReviewDtos.ReviewResponse> getSellerReviews(Long sellerId) {
        return reviewRepository.findBySellerId(sellerId).stream()
                .map(ReviewService::toResponse)
                .toList();
    }

    public static ReviewDtos.ReviewResponse toResponse(Review r) {
        return new ReviewDtos.ReviewResponse(
                r.getId(),
                r.getFulfillmentOrderId(),
                r.getUnitId(),
                r.getSellerId(),
                r.getBuyerId(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
