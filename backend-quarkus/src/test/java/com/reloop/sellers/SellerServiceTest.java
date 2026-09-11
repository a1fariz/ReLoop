package com.reloop.sellers;

import com.reloop.disputes.repository.DisputeRepository;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.sellers.dto.SellerMetricsResponse;
import com.reloop.sellers.service.SellerReputationService;
import com.reloop.sellers.service.SellerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private com.reloop.reviews.repository.ReviewRepository reviewRepository;

    private SellerService sellerService;

    @BeforeEach
    void setUp() {
        sellerService = new SellerService(fulfillmentOrderRepository, disputeRepository,
                reviewRepository, new SellerReputationService());
    }

    @Test
    @DisplayName("Metrics aggregate real fulfillment/dispute counts into the Bayesian score")
    void testMetricsFromRealData() {
        when(reviewRepository.averageRatingBySellerId(20L)).thenReturn(4.9);
        when(fulfillmentOrderRepository.count(
                "sellerId = ?1 AND fulfillmentStatus = ?2", 20L, FulfillmentOrder.FulfillmentStatus.COMPLETED))
                .thenReturn(100L);
        when(disputeRepository.count("sellerId = ?1 AND status IN (?2, ?3, ?4)", 20L,
                com.reloop.disputes.domain.Dispute.DisputeStatus.OPEN,
                com.reloop.disputes.domain.Dispute.DisputeStatus.UNDER_REVIEW,
                com.reloop.disputes.domain.Dispute.DisputeStatus.EVIDENCE_COLLECTING))
                .thenReturn(1L);
        when(disputeRepository.count("sellerId = ?1 AND status IN (?2, ?3)", 20L,
                com.reloop.disputes.domain.Dispute.DisputeStatus.RESOLVED,
                com.reloop.disputes.domain.Dispute.DisputeStatus.CLOSED))
                .thenReturn(2L);

        SellerMetricsResponse metrics = sellerService.getSellerMetrics(20L);

        assertThat(metrics.completedOrders()).isEqualTo(100);
        assertThat(metrics.activeDisputes()).isEqualTo(1);
        // disputeRate = 2 resolved / 100 completed = 0.020
        assertThat(metrics.disputeRate()).isEqualByComparingTo(new BigDecimal("0.020"));
        // 39.2 (rating) + 25 (quality) + 19.6 (trust) + 14.85 (response) = 98.65, minus 1 open dispute * 5
        assertThat(metrics.reputationScore()).isEqualByComparingTo(new BigDecimal("93.65"));
    }

    @Test
    @DisplayName("New seller with zero orders sits at the neutral 50.00 baseline (no cold-start distortion)")
    void testColdStartBaseline() {
        when(reviewRepository.averageRatingBySellerId(20L)).thenReturn(null);
        when(fulfillmentOrderRepository.count(
                "sellerId = ?1 AND fulfillmentStatus = ?2", 20L, FulfillmentOrder.FulfillmentStatus.COMPLETED))
                .thenReturn(0L);
        when(disputeRepository.count("sellerId = ?1 AND status IN (?2, ?3, ?4)", 20L,
                com.reloop.disputes.domain.Dispute.DisputeStatus.OPEN,
                com.reloop.disputes.domain.Dispute.DisputeStatus.UNDER_REVIEW,
                com.reloop.disputes.domain.Dispute.DisputeStatus.EVIDENCE_COLLECTING))
                .thenReturn(0L);
        when(disputeRepository.count("sellerId = ?1 AND status IN (?2, ?3)", 20L,
                com.reloop.disputes.domain.Dispute.DisputeStatus.RESOLVED,
                com.reloop.disputes.domain.Dispute.DisputeStatus.CLOSED))
                .thenReturn(0L);

        SellerMetricsResponse metrics = sellerService.getSellerMetrics(20L);

        assertThat(metrics.completedOrders()).isZero();
        assertThat(metrics.reputationScore()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
}
