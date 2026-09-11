package com.reloop.inspections;

import com.reloop.inspections.service.GradingCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradingCalculatorServiceTest {
    private GradingCalculatorService gradingCalculator;

    @BeforeEach
    void setUp() {
        gradingCalculator = new GradingCalculatorService();
    }

    @Test
    @DisplayName("Should assign Grade A+ for flawless scores")
    void testGradeAPlus() {
        String grade = gradingCalculator.calculateGrade(98, 96, 100, false);
        assertThat(grade).isEqualTo("A+");
    }

    @Test
    @DisplayName("Critical hardware failure immediately downgrades unit to Grade D")
    void testCriticalFailureForcesGradeD() {
        String grade = gradingCalculator.calculateGrade(99, 99, 99, true); // hasCriticalFailure = true
        assertThat(grade).isEqualTo("D");
    }

    @Test
    @DisplayName("Minor wear assigns Grade B+")
    void testMinorWearGradeBPlus() {
        String grade = gradingCalculator.calculateGrade(80, 85, 90, false);
        assertThat(grade).isEqualTo("B+");
    }
}
