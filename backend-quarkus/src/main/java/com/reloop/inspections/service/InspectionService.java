package com.reloop.inspections.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.inspections.domain.TechnicalInspection;
import com.reloop.inspections.dto.InspectionDtos;
import com.reloop.inspections.repository.TechnicalInspectionRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class InspectionService {

    private final TechnicalInspectionRepository inspectionRepository;
    private final ProductUnitRepository productUnitRepository;
    private final GradingCalculatorService gradingCalculator;

    @Inject
    public InspectionService(
            TechnicalInspectionRepository inspectionRepository,
            ProductUnitRepository productUnitRepository,
            GradingCalculatorService gradingCalculator
    ) {
        this.inspectionRepository = inspectionRepository;
        this.productUnitRepository = productUnitRepository;
        this.gradingCalculator = gradingCalculator;
    }

    @Transactional
    public InspectionDtos.InspectionResponse createInspection(Long technicianId, InspectionDtos.CreateInspectionRequest request) {
        ProductUnit unit = productUnitRepository.findByIdOptional(request.unitId())
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        String grade = gradingCalculator.calculateGrade(
                request.physicalScore(), request.hardwareScore(), request.softwareScore(), request.hasCriticalFailure());

        TechnicalInspection inspection = new TechnicalInspection(
                request.unitId(),
                technicianId,
                null,
                request.physicalScore(),
                request.hardwareScore(),
                request.softwareScore(),
                grade,
                request.estimatedRepairCost() != null ? request.estimatedRepairCost() : BigDecimal.ZERO,
                request.technicianNotes()
        );
        inspection = inspectionRepository.save(inspection);

        // The certified grade flows back onto the serialized unit for listings/trade-ins
        unit.setGrade(grade);
        productUnitRepository.save(unit);

        return toResponse(inspection);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public InspectionDtos.InspectionResponse getLatestByUnit(UUID unitId) {
        return inspectionRepository.findTopByUnitIdOrderByCreatedAtDesc(unitId)
                .map(InspectionService::toResponse)
                .orElseThrow(() -> new BusinessException("No inspection found for this unit", "INSPECTION_NOT_FOUND", 404));
    }

    public static InspectionDtos.InspectionResponse toResponse(TechnicalInspection i) {
        return new InspectionDtos.InspectionResponse(
                i.getId(),
                i.getUnitId(),
                i.getTechnicianId(),
                i.getPhysicalScore(),
                i.getHardwareScore(),
                i.getSoftwareScore(),
                i.getFinalCalculatedGrade(),
                i.getEstimatedRepairCost(),
                i.getTechnicianNotes(),
                i.getCreatedAt()
        );
    }
}
