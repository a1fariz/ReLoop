package com.reloop.sellers;

import com.reloop.sellers.service.SellerReputationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SellerReputationServiceTest {
    private SellerReputationService reputationService;

    @BeforeEach
    void setUp() {
        reputationService = new SellerReputationService();
    }

    @Test
    @DisplayName("New seller with zero orders starts at neutral baseline score 50.00 (Zero Cold-Start Distortion)")
    void testNewSellerColdStartNeutralScore() {
        BigDecimal score = reputationService.calculateReputationScore(
                0.0, // rating
                0,   // completed orders
                0.0, // return rate
                0.0, // dispute rate
                1.0, // response rate
                0    // active disputes
        );

        assertThat(score).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Experienced seller with 100 orders and top performance reaches high reputation")
    void testTopSellerHighReputation() {
        BigDecimal score = reputationService.calculateReputationScore(
                5.0,  // rating (40 pts)
                100,  // completed orders (Volume weight = 1.0)
                0.01, // return rate (24.75 pts)
                0.00, // dispute rate (20 pts)
                0.98, // response rate (14.7 pts)
                0     // active disputes
        );

        // Expected score >= 95
        assertThat(score).isGreaterThan(new BigDecimal("95.00"));
        assertThat(score).isLessThanOrEqualTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Active dispute heavily penalizes seller reputation")
    void testActiveDisputePenalty() {
        BigDecimal baseScore = reputationService.calculateReputationScore(4.5, 50, 0.05, 0.02, 0.90, 0);
        BigDecimal penalizedScore = reputationService.calculateReputationScore(4.5, 50, 0.05, 0.02, 0.90, 3); // 3 active disputes = -15 pts

        assertThat(penalizedScore).isEqualByComparingTo(baseScore.subtract(new BigDecimal("15.00")));
    }
}
