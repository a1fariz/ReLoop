package com.reloop.sellers.service;

import com.reloop.disputes.domain.Dispute;
import com.reloop.disputes.repository.DisputeRepository;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.reviews.repository.ReviewRepository;
import com.reloop.sellers.dto.SellerMetricsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes seller reputation metrics from real fulfillment and dispute data.
 * Return rate and average rating remain placeholders until a returns/review
 * module exists (documented in IMPROVEMENT_SUGGESTIONS.md).
 */
@ApplicationScoped
public class SellerService {

    @PersistenceContext
    EntityManager entityManager;

    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final DisputeRepository disputeRepository;
    private final ReviewRepository reviewRepository;
    private final SellerReputationService reputationService;

    @Inject
    public SellerService(
            FulfillmentOrderRepository fulfillmentOrderRepository,
            DisputeRepository disputeRepository,
            ReviewRepository reviewRepository,
            SellerReputationService reputationService
    ) {
        this.fulfillmentOrderRepository = fulfillmentOrderRepository;
        this.disputeRepository = disputeRepository;
        this.reviewRepository = reviewRepository;
        this.reputationService = reputationService;
    }

    /**
     * The API is keyed by user id (RBAC subject); fulfillment/review rows
     * reference the seller profile id. Translate before aggregating.
     * Falls back to the user id itself when no seller profile row exists
     * (or when no EntityManager is bound, e.g. in plain unit tests).
     */
    private Long sellerProfileId(Long sellerUserId) {
        if (entityManager == null) return sellerUserId;
        Object result = entityManager
                .createNativeQuery("SELECT id FROM sellers WHERE user_id = ?1")
                .setParameter(1, sellerUserId)
                .getResultList().stream().findFirst().orElse(null);
        if (result == null) return sellerUserId;
        if (result instanceof Number n) return n.longValue();
        return Long.parseLong(result.toString());
    }

    public SellerMetricsResponse getSellerMetrics(Long sellerUserId) {
        Long sellerId = sellerProfileId(sellerUserId);
        long completedOrders = fulfillmentOrderRepository.count(
                "sellerId = ?1 AND fulfillmentStatus = ?2", sellerId, FulfillmentOrder.FulfillmentStatus.COMPLETED);

        long openDisputes = disputeRepository.count(
                "sellerId = ?1 AND status IN (?2, ?3, ?4)", sellerId,
                Dispute.DisputeStatus.OPEN, Dispute.DisputeStatus.UNDER_REVIEW, Dispute.DisputeStatus.EVIDENCE_COLLECTING);

        long resolvedDisputes = disputeRepository.count(
                "sellerId = ?1 AND status IN (?2, ?3)", sellerId,
                Dispute.DisputeStatus.RESOLVED, Dispute.DisputeStatus.CLOSED);

        double disputeRate = completedOrders == 0
                ? 0.0
                : Math.min(1.0, resolvedDisputes / (double) completedOrders);

        Double avgRating = reviewRepository.averageRatingBySellerId(sellerId);
        double rating = avgRating != null ? avgRating : 4.9; // baseline until first reviews arrive

        BigDecimal reputation = reputationService.calculateReputationScore(
                rating,
                (int) completedOrders,
                0.0,                       // return rate placeholder (no returns data yet)
                disputeRate,
                0.99,                      // response rate placeholder (no messaging data yet)
                (int) openDisputes);

        return new SellerMetricsResponse(
                sellerUserId,
                "Seller Store",
                "seller-" + sellerUserId,
                reputation,
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.valueOf(disputeRate).setScale(3, RoundingMode.HALF_UP),
                new BigDecimal("0.990"),
                (int) completedOrders,
                (int) openDisputes,
                "VERIFIED"
        );
    }
}
