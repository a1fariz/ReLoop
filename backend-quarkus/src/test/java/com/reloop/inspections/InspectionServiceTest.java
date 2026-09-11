package com.reloop.inspections;

import com.reloop.common.exception.BusinessException;
import com.reloop.inspections.domain.TechnicalInspection;
import com.reloop.inspections.dto.InspectionDtos;
import com.reloop.inspections.repository.TechnicalInspectionRepository;
import com.reloop.inspections.service.GradingCalculatorService;
import com.reloop.inspections.service.InspectionService;
import com.reloop.support.TestFields;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
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
class InspectionServiceTest {

    @Mock
    private TechnicalInspectionRepository inspectionRepository;

    @Mock
    private ProductUnitRepository productUnitRepository;

    private InspectionService inspectionService;

    @BeforeEach
    void setUp() {
        inspectionService = new InspectionService(
                inspectionRepository,
                productUnitRepository,
                new GradingCalculatorService());
    }

    private ProductUnit unit() {
        ProductUnit unit = new ProductUnit(UUID.randomUUID(), "SN-INSPECT", 77L,
                ProductUnit.UnitStatus.TRADE_IN_PENDING, null);
        TestFields.set(unit, "id", UUID.randomUUID());
        return unit;
    }

    private InspectionDtos.CreateInspectionRequest request(UUID unitId) {
        return new InspectionDtos.CreateInspectionRequest(
                unitId, 98, 96, 100, false, BigDecimal.ZERO, "Looks brand new");
    }

    @Test
    @DisplayName("Flawless scores produce Grade A+ and the grade flows onto the unit")
    void testCreateInspectionGradeAPlus() {
        ProductUnit unit = unit();
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));
        when(inspectionRepository.save(any(TechnicalInspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productUnitRepository.save(any(ProductUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InspectionDtos.InspectionResponse response = inspectionService.createInspection(55L, request(unit.getId()));

        assertThat(response.finalCalculatedGrade()).isEqualTo("A+");
        assertThat(unit.getGrade()).isEqualTo("A+");
    }

    @Test
    @DisplayName("Critical failure downgrades the unit to Grade D")
    void testCreateInspectionCriticalFailure() {
        ProductUnit unit = unit();
        when(productUnitRepository.findByIdOptional(unit.getId())).thenReturn(Optional.of(unit));
        when(inspectionRepository.save(any(TechnicalInspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productUnitRepository.save(any(ProductUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InspectionDtos.CreateInspectionRequest critical = new InspectionDtos.CreateInspectionRequest(
                unit.getId(), 99, 99, 99, true, new BigDecimal("500000"), "Motherboard corroded");

        InspectionDtos.InspectionResponse response = inspectionService.createInspection(55L, critical);

        assertThat(response.finalCalculatedGrade()).isEqualTo("D");
        assertThat(unit.getGrade()).isEqualTo("D");
    }

    @Test
    @DisplayName("Inspection for an unknown unit is rejected")
    void testCreateInspectionUnknownUnit() {
        UUID unitId = UUID.randomUUID();
        when(productUnitRepository.findByIdOptional(unitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inspectionService.createInspection(55L, request(unitId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Product unit not found");
    }

    @Test
    @DisplayName("Latest inspection lookup for a unit without inspections is 404")
    void testGetLatestMissing() {
        UUID unitId = UUID.randomUUID();
        when(inspectionRepository.findTopByUnitIdOrderByCreatedAtDesc(unitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inspectionService.getLatestByUnit(unitId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No inspection found");
    }

    @Test
    @DisplayName("Latest inspection lookup returns the stored record")
    void testGetLatestPresent() {
        UUID unitId = UUID.randomUUID();
        TechnicalInspection inspection = new TechnicalInspection(unitId, 55L, null,
                80, 85, 90, "B+", BigDecimal.ZERO, "Minor wear");
        when(inspectionRepository.findTopByUnitIdOrderByCreatedAtDesc(unitId)).thenReturn(Optional.of(inspection));

        InspectionDtos.InspectionResponse response = inspectionService.getLatestByUnit(unitId);

        assertThat(response.finalCalculatedGrade()).isEqualTo("B+");
        assertThat(response.technicianId()).isEqualTo(55L);
    }
}
