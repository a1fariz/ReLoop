package com.reloop.inspections.service;

import org.springframework.stereotype.Service;

@Service
public class GradingCalculatorService {

    public String calculateGrade(int physicalScore, int hardwareScore, int softwareScore, boolean hasCriticalFailure) {
        if (hasCriticalFailure) {
            return "D";
        }

        // Standard Weighted Formula: Physical (40%) + Hardware (40%) + Software (20%)
        double finalScore = (physicalScore * 0.40) + (hardwareScore * 0.40) + (softwareScore * 0.20);

        if (finalScore >= 95.0) return "A+";
        if (finalScore >= 90.0) return "A";
        if (finalScore >= 80.0) return "B+";
        if (finalScore >= 70.0) return "B";
        if (finalScore >= 60.0) return "C";
        return "D";
    }
}
