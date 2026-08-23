package com.reloop.sellers.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SellerReputationService {

    public BigDecimal calculateReputationScore(
            double averageRating,      // 0.0 - 5.0
            int completedOrders,       // >= 0
            double returnRate,         // 0.0 - 1.0
            double disputeRate,        // 0.0 - 1.0
            double responseRate,       // 0.0 - 1.0
            int activeDisputes         // >= 0
    ) {
        // Bayesian volume dampener to eliminate cold-start bias
        double volumeWeight = Math.min(completedOrders / 50.0, 1.0);

        double ratingScore = (Math.min(averageRating, 5.0) / 5.0) * 40.0;    // Max 40 pts
        double qualityScore = Math.max(0.0, (1.0 - returnRate)) * 25.0;      // Max 25 pts
        double trustScore = Math.max(0.0, (1.0 - disputeRate)) * 20.0;       // Max 20 pts
        double responseScore = Math.min(responseRate, 1.0) * 15.0;          // Max 15 pts

        double calculatedPerformance = ratingScore + qualityScore + trustScore + responseScore;

        // Smooth transition from baseline neutral 50.0 to real performance
        double reputationScore = (50.0 * (1.0 - volumeWeight)) + (calculatedPerformance * volumeWeight);

        // Penalty for unresolved active disputes
        double finalScore = reputationScore - (activeDisputes * 5.0);

        // Clamp between 0.00 and 100.00
        double clamped = Math.max(0.0, Math.min(100.0, finalScore));

        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
    }
}
