package com.reloop.warranties.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.warranties.domain.Warranty;
import com.reloop.warranties.dto.WarrantyDto;
import com.reloop.warranties.repository.WarrantyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WarrantyService {
    private final WarrantyRepository warrantyRepository;

    @Inject
    public WarrantyService(WarrantyRepository warrantyRepository) {
        this.warrantyRepository = warrantyRepository;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<WarrantyDto> getUserWarranties(Long ownerId) {
        return warrantyRepository.findByOwnerId(ownerId).stream()
                .map(WarrantyService::toDto)
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<WarrantyDto> getUserWarrantiesPaged(Long ownerId, int page, int size) {
        var query = warrantyRepository.find("ownerId = ?1",
                io.quarkus.panache.common.Sort.descending("startsAt"), ownerId);
        long total = query.count();
        List<Warranty> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        return new com.reloop.common.dto.Page<>(items.stream().map(WarrantyService::toDto).toList(), total, page, size);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public WarrantyDto getWarrantyByUnitId(UUID unitId) {
        return warrantyRepository.findByUnitIdAndIsVoidedFalse(unitId)
                .map(WarrantyService::toDto)
                .orElseThrow(() -> new BusinessException("Active warranty not found", "WARRANTY_NOT_FOUND", 404));
    }

    /**
     * Issues the standard 6-month policy for a delivered unit. Idempotent: if the
     * unit already has a non-voided warranty, that one is returned unchanged.
     */
    @Transactional
    public Warranty issueStandardWarranty(UUID unitId, Long ownerId, UUID fulfillmentOrderId) {
        Warranty existing = warrantyRepository.findByUnitIdAndIsVoidedFalse(unitId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        Warranty warranty = new Warranty(unitId, ownerId, fulfillmentOrderId,
                now, now.plus(182, java.time.temporal.ChronoUnit.DAYS), "STANDARD_6_MONTHS");
        return warrantyRepository.save(warranty);
    }

    public static WarrantyDto toDto(Warranty w) {
        return new WarrantyDto(
                w.getId(),
                w.getUnitId(),
                w.getOwnerId(),
                w.getFulfillmentOrderId(),
                w.getStartsAt(),
                w.getExpiresAt(),
                w.getPolicyTier(),
                w.isVoided()
        );
    }
}
