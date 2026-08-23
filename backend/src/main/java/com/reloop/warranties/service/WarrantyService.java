package com.reloop.warranties.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.warranties.domain.Warranty;
import com.reloop.warranties.dto.WarrantyDto;
import com.reloop.warranties.repository.WarrantyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarrantyService {
    private final WarrantyRepository warrantyRepository;

    public WarrantyService(WarrantyRepository warrantyRepository) {
        this.warrantyRepository = warrantyRepository;
    }

    @Transactional(readOnly = true)
    public List<WarrantyDto> getUserWarranties(Long ownerId) {
        return warrantyRepository.findByOwnerId(ownerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarrantyDto getWarrantyByUnitId(UUID unitId) {
        return warrantyRepository.findByUnitIdAndIsVoidedFalse(unitId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException("Active warranty not found", "WARRANTY_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private WarrantyDto toDto(Warranty w) {
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
