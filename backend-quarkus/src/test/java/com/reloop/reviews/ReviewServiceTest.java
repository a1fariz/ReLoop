package com.reloop.reviews;

import com.reloop.common.exception.BusinessException;
import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.orders.domain.MasterOrder;
import com.reloop.orders.repository.FulfillmentOrderRepository;
import com.reloop.orders.repository.MasterOrderRepository;
import com.reloop.reviews.dto.ReviewDtos;
import com.reloop.reviews.repository.ReviewRepository;
import com.reloop.reviews.service.ReviewService;
import com.reloop.support.TestFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    private MasterOrderRepository masterOrderRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, fulfillmentOrderRepository, masterOrderRepository);
    }

    private FulfillmentOrder completedOrder(Long buyerId) {
        MasterOrder masterOrder = new MasterOrder("ORD-1", buyerId, new BigDecimal("10000000.00"), "{}");
        TestFields.set(masterOrder, "id", UUID.randomUUID());
        FulfillmentOrder fulfillment = new FulfillmentOrder(masterOrder.getId(), 20L, UUID.randomUUID(),
                new BigDecimal("10000000.00"), new BigDecimal("1500000.00"), new BigDecimal("8500000.00"));
        TestFields.set(fulfillment, "id", UUID.randomUUID());
        fulfillment.setFulfillmentStatus(FulfillmentOrder.FulfillmentStatus.COMPLETED);
        return fulfillment;
    }

    @Test
    @DisplayName("Buyer of a completed order can review it once")
    void testSubmitReviewSuccess() {
        FulfillmentOrder fulfillment = completedOrder(10L);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId()))
                .thenReturn(Optional.of(new MasterOrder("ORD-1", 10L, new BigDecimal("10000000.00"), "{}")));
        when(reviewRepository.findByFulfillmentOrderId(fulfillment.getId())).thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDtos.ReviewResponse response = reviewService.submitReview(10L,
                new ReviewDtos.SubmitReviewRequest(fulfillment.getId(), 5, "Great seller"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.sellerId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Only the buyer can review the order")
    void testSubmitReviewNotBuyer() {
        FulfillmentOrder fulfillment = completedOrder(10L);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId()))
                .thenReturn(Optional.of(new MasterOrder("ORD-1", 10L, new BigDecimal("10000000.00"), "{}")));

        assertThatThrownBy(() -> reviewService.submitReview(99L,
                new ReviewDtos.SubmitReviewRequest(fulfillment.getId(), 5, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only the buyer");
    }

    @Test
    @DisplayName("Reviewing an order twice is rejected")
    void testSubmitReviewTwice() {
        FulfillmentOrder fulfillment = completedOrder(10L);
        when(fulfillmentOrderRepository.findByIdOptional(fulfillment.getId())).thenReturn(Optional.of(fulfillment));
        when(masterOrderRepository.findByIdOptional(fulfillment.getMasterOrderId()))
                .thenReturn(Optional.of(new MasterOrder("ORD-1", 10L, new BigDecimal("10000000.00"), "{}")));
        when(reviewRepository.findByFulfillmentOrderId(fulfillment.getId()))
                .thenReturn(Optional.of(new com.reloop.reviews.domain.Review(
                        fulfillment.getId(), fulfillment.getUnitId(), 20L, 10L, 5, null)));

        assertThatThrownBy(() -> reviewService.submitReview(10L,
                new ReviewDtos.SubmitReviewRequest(fulfillment.getId(), 4, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been reviewed");
    }
}
