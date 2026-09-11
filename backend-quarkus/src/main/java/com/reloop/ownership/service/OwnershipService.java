package com.reloop.ownership.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.dto.Page;
import com.reloop.common.exception.BusinessException;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.ownership.domain.OwnershipTransfer;
import com.reloop.ownership.dto.OwnershipDtos;
import com.reloop.ownership.repository.OwnershipTransferRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Append-only legal ownership provenance chain. Every custody-changing event
 * (purchase, trade-in, return, platform recovery) records one immutable row
 * per unit, giving an auditable chain of title.
 */
@ApplicationScoped
public class OwnershipService {
    private static final Logger log = Logger.getLogger(OwnershipService.class);

    private final OwnershipTransferRepository ownershipTransferRepository;
    private final ProductUnitRepository productUnitRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public OwnershipService(
            OwnershipTransferRepository ownershipTransferRepository,
            ProductUnitRepository productUnitRepository,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.ownershipTransferRepository = ownershipTransferRepository;
        this.productUnitRepository = productUnitRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public OwnershipDtos.OwnershipTransferDto recordTransfer(
            UUID unitId, Long fromOwnerId, Long toOwnerId, OwnershipTransfer.TransferType transferType,
            String referenceType, UUID referenceId) {
        // Same unit-loading mechanism as InspectionService / OrderFulfillmentService
        ProductUnit unit = productUnitRepository.findByIdOptional(unitId)
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        OwnershipTransfer transfer = ownershipTransferRepository.save(new OwnershipTransfer(
                unitId, fromOwnerId, toOwnerId, transferType, referenceType, referenceId));

        auditService.record("OwnershipTransfer", transfer.getId().toString(), "OWNERSHIP_TRANSFERRED", null,
                null, fromOwnerId != null ? String.valueOf(fromOwnerId) : null, String.valueOf(toOwnerId));
        emitOwnershipTransferred(transfer);
        return toDto(transfer);
    }

    /**
     * IDOR: ADMIN, or the caller appears anywhere in the chain (as from_owner
     * or to_owner). Everyone else gets a 403.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public OwnershipDtos.OwnershipChainDto getUnitProvenance(UUID unitId, Long callerId, boolean isAdmin) {
        ProductUnit unit = productUnitRepository.findByIdOptional(unitId)
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        List<OwnershipTransfer> transfers = ownershipTransferRepository.findByUnitIdOrderByTransferredAtDesc(unitId);
        if (!isAdmin) {
            boolean involved = transfers.stream().anyMatch(t ->
                    callerId.equals(t.getFromOwnerId()) || callerId.equals(t.getToOwnerId()));
            if (!involved) {
                throw new BusinessException("Only participants in this unit's ownership chain may view its provenance",
                        "OWNERSHIP_FORBIDDEN", 403);
            }
        }
        return new OwnershipDtos.OwnershipChainDto(unitId,
                transfers.stream().map(OwnershipService::toDto).toList(),
                unit.getCurrentOwnerId(),
                true,
                List.of());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<OwnershipDtos.OwnershipTransferDto> getMyTransfers(Long userId, int page, int size) {
        Page<OwnershipTransfer> result = ownershipTransferRepository.findPagedByToOwnerId(userId, page, size);
        return new Page<>(result.items().stream().map(OwnershipService::toDto).toList(), result.total(), page, size);
    }

    /**
     * ADMIN chain integrity check: non-empty chain, strictly increasing
     * transferred_at, and the unit's current owner matches the latest
     * PURCHASE to_owner_id.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public OwnershipDtos.OwnershipChainDto verifyChain(UUID unitId) {
        ProductUnit unit = productUnitRepository.findByIdOptional(unitId)
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        List<OwnershipTransfer> transfers = ownershipTransferRepository.findByUnitIdOrderByTransferredAtDesc(unitId);
        List<String> issues = new ArrayList<>();

        if (transfers.isEmpty()) {
            issues.add("No ownership transfers recorded for this unit");
        } else {
            // Descending list: verify each older transfer happened strictly before the next newer one
            for (int i = 1; i < transfers.size(); i++) {
                OwnershipTransfer newer = transfers.get(i - 1);
                OwnershipTransfer older = transfers.get(i);
                if (!older.getTransferredAt().isBefore(newer.getTransferredAt())) {
                    issues.add("Chronology broken between transfer " + older.getId() + " and " + newer.getId());
                }
            }
            OwnershipTransfer latest = transfers.get(0);
            if (latest.getTransferType() == OwnershipTransfer.TransferType.PURCHASE
                    && !unit.getCurrentOwnerId().equals(latest.getToOwnerId())) {
                issues.add("Unit currentOwnerId " + unit.getCurrentOwnerId()
                        + " does not match latest PURCHASE to_owner_id " + latest.getToOwnerId());
            }
        }

        return new OwnershipDtos.OwnershipChainDto(unitId,
                transfers.stream().map(OwnershipService::toDto).toList(),
                unit.getCurrentOwnerId(),
                issues.isEmpty(),
                issues);
    }

    private void emitOwnershipTransferred(OwnershipTransfer transfer) {
        try {
            String payload = objectMapper.writeValueAsString(new OwnershipTransferredPayload(
                    transfer.getUnitId(),
                    transfer.getFromOwnerId(),
                    transfer.getToOwnerId(),
                    transfer.getTransferType().name()
            ));
            outboxEventRepository.save(new OutboxEvent(
                    "OWNERSHIP",
                    transfer.getUnitId().toString(),
                    "OWNERSHIP_TRANSFERRED",
                    payload,
                    instanceCorrelationId,
                    "OWNERSHIP_TRANSFERRED:" + transfer.getUnitId() + ":" + transfer.getToOwnerId()
                            + ":" + transfer.getTransferType()
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize OWNERSHIP_TRANSFERRED payload for unit %s", transfer.getUnitId());
        }
    }

    public static OwnershipDtos.OwnershipTransferDto toDto(OwnershipTransfer t) {
        return new OwnershipDtos.OwnershipTransferDto(
                t.getId(),
                t.getUnitId(),
                t.getFromOwnerId(),
                t.getToOwnerId(),
                t.getTransferType() != null ? t.getTransferType().name() : null,
                t.getReferenceType(),
                t.getReferenceId(),
                t.getTransferredAt()
        );
    }

    record OwnershipTransferredPayload(
            UUID unitId,
            Long fromOwnerId,
            Long toOwnerId,
            String transferType
    ) {}
}
