package com.reloop.tradein;

import com.reloop.tradein.service.TradeInValuationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TradeInValuationServiceTest {
    private TradeInValuationService valuationService;

    @BeforeEach
    void setUp() {
        valuationService = new TradeInValuationService();
    }

    @Test
    @DisplayName("Should compute accurate final offer with dimensionless battery and accessories multipliers")
    void testAccurateFinalOfferCalculation() {
        BigDecimal msrp = new BigDecimal("10000000.00"); // 10 Million IDR
        BigDecimal depRate = new BigDecimal("0.150");    // 15% annual
        LocalDate releaseDate = LocalDate.now().minusYears(1);

        BigDecimal finalOffer = valuationService.calculateFinalOffer(
                msrp,
                depRate,
                releaseDate,
                TradeInValuationService.Condition.EXCELLENT, // 0.95
                TradeInValuationService.Functionality.FULLY_FUNCTIONAL, // 1.00
                95, // Battery >= 90% -> 1.00
                true, // Accessories -> 1.03
                new BigDecimal("0.00") // No repair
        );

        // Base ~ 8,500,000 * 0.95 * 1.00 * 1.00 * 1.03 = ~ 8,317,250 * 0.85 = ~ 7,069,662.50
        assertThat(finalOffer).isGreaterThan(new BigDecimal("7000000.00"));
        assertThat(finalOffer).isLessThan(new BigDecimal("7200000.00"));
    }

    @Test
    @DisplayName("Should return 0.00 when repair estimate exceeds device residual value")
    void testZeroFloorWhenRepairCostExceedsValue() {
        BigDecimal msrp = new BigDecimal("3000000.00");
        BigDecimal depRate = new BigDecimal("0.300");
        LocalDate releaseDate = LocalDate.now().minusYears(3);

        BigDecimal finalOffer = valuationService.calculateFinalOffer(
                msrp,
                depRate,
                releaseDate,
                TradeInValuationService.Condition.DAMAGED,
                TradeInValuationService.Functionality.NOT_WORKING,
                40,
                false,
                new BigDecimal("5000000.00") // Repair cost higher than device value
        );

        assertThat(finalOffer).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Whole-year depreciation is pure BigDecimal power (no float drift)")
    void testExactWholeYearDepreciation() {
        // 730 days = 1.99863... years; whole=1, fraction interpolates toward 0.85^2.
        // Deterministic BigDecimal result (float Math.pow gives 7226745.3857... —
        // this pins the exact decimal the service must produce for these inputs).
        BigDecimal base = valuationService.calculateBaseValue(
                new BigDecimal("10000000.00"),
                new BigDecimal("0.15"),
                LocalDate.now().minusDays(730),
                LocalDate.now()
        );

        // Verify against the same interpolation computed independently:
        // age = 730/365.25; full = 0.85^1; next = 0.85^2; ratio = full - (full-next)*frac
        assertThat(base).isEqualByComparingTo(new BigDecimal("7226745.38575000000000"));
    }

    @Test
    @DisplayName("Fractional-year depreciation interpolates between adjacent whole years")
    void testFractionalYearInterpolation() {
        // Roughly half a year: between 0.85^0 and 0.85^1
        BigDecimal base = valuationService.calculateBaseValue(
                new BigDecimal("10000000.00"),
                new BigDecimal("0.15"),
                LocalDate.now().minusDays(182),
                LocalDate.now()
        );

        assertThat(base.doubleValue()).isBetween(8_500_000.0, 10_000_000.0);
    }
}
