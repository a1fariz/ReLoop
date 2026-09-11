package com.reloop.tradein.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class TradeInValuationService {
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal PLATFORM_MARGIN_RATE = new BigDecimal("0.15"); // 15% platform margin

    public enum Condition {
        EXCELLENT(new BigDecimal("0.95")),
        GOOD(new BigDecimal("0.85")),
        FAIR(new BigDecimal("0.70")),
        POOR(new BigDecimal("0.50")),
        DAMAGED(new BigDecimal("0.30"));

        private final BigDecimal multiplier;
        Condition(BigDecimal multiplier) { this.multiplier = multiplier; }
        public BigDecimal getMultiplier() { return multiplier; }
    }

    public enum Functionality {
        FULLY_FUNCTIONAL(new BigDecimal("1.00")),
        MINOR_ISSUES(new BigDecimal("0.80")),
        MAJOR_ISSUES(new BigDecimal("0.50")),
        NOT_WORKING(new BigDecimal("0.20"));

        private final BigDecimal multiplier;
        Functionality(BigDecimal multiplier) { this.multiplier = multiplier; }
        public BigDecimal getMultiplier() { return multiplier; }
    }

    /**
     * Depreciated MSRP: msrp * (1 - rate)^age, computed entirely in BigDecimal.
     * Fractional years are interpolated linearly between the surrounding integer
     * powers (365.25-day years) — no floating point ever touches the money path.
     */
    public BigDecimal calculateBaseValue(BigDecimal msrp, BigDecimal annualDepreciationRate, LocalDate releaseDate, LocalDate calculationDate) {
        long days = ChronoUnit.DAYS.between(releaseDate, calculationDate);
        if (days < 0) days = 0;

        BigDecimal ageInYears = BigDecimal.valueOf(days)
                .divide(BigDecimal.valueOf(365.25), 8, RoundingMode.HALF_UP);
        long wholeYears = ageInYears.setScale(0, RoundingMode.FLOOR).longValueExact();
        BigDecimal fraction = ageInYears.subtract(BigDecimal.valueOf(wholeYears));

        BigDecimal depFactor = BigDecimal.ONE.subtract(annualDepreciationRate, MC);
        BigDecimal fullFactor = depFactor.pow(Math.toIntExact(wholeYears), MC);
        BigDecimal nextFactor = depFactor.pow(Math.toIntExact(wholeYears + 1), MC);
        BigDecimal remainingRatio = fullFactor
                .subtract(fullFactor.subtract(nextFactor, MC).multiply(fraction, MC), MC);

        return msrp.multiply(remainingRatio, MC);
    }

    public BigDecimal getBatteryMultiplier(int batteryHealthPercentage) {
        if (batteryHealthPercentage >= 90) return new BigDecimal("1.00");
        if (batteryHealthPercentage >= 80) return new BigDecimal("0.98");
        if (batteryHealthPercentage >= 70) return new BigDecimal("0.95");
        if (batteryHealthPercentage >= 60) return new BigDecimal("0.90");
        return new BigDecimal("0.85");
    }

    public BigDecimal calculateFinalOffer(
            BigDecimal msrp,
            BigDecimal annualDepreciationRate,
            LocalDate releaseDate,
            Condition condition,
            Functionality functionality,
            int batteryHealthPercentage,
            boolean hasAccessories,
            BigDecimal repairEstimate
    ) {
        BigDecimal baseValue = calculateBaseValue(msrp, annualDepreciationRate, releaseDate, LocalDate.now());
        BigDecimal batteryMultiplier = getBatteryMultiplier(batteryHealthPercentage);
        BigDecimal accessoriesMultiplier = hasAccessories ? new BigDecimal("1.03") : BigDecimal.ONE;

        // Dimensionless Multipliers: Base * Condition * Functionality * Battery * Accessories
        BigDecimal adjustedValue = baseValue
                .multiply(condition.getMultiplier(), MC)
                .multiply(functionality.getMultiplier(), MC)
                .multiply(batteryMultiplier, MC)
                .multiply(accessoriesMultiplier, MC);

        BigDecimal repairCost = repairEstimate != null ? repairEstimate : BigDecimal.ZERO;
        BigDecimal netValue = adjustedValue.subtract(repairCost, MC);

        if (netValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal finalOffer = netValue.multiply(BigDecimal.ONE.subtract(PLATFORM_MARGIN_RATE), MC);
        return finalOffer.setScale(2, RoundingMode.HALF_UP);
    }
}
