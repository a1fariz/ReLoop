package com.reloop.refurbishment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.dto.Page;
import com.reloop.common.exception.BusinessException;
import com.reloop.inspections.dto.InspectionDtos;
import com.reloop.inspections.service.InspectionService;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.refurbishment.domain.RepairTicket;
import com.reloop.refurbishment.dto.RepairDtos;
import com.reloop.refurbishment.repository.RepairTicketRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repair ticket lifecycle: OPEN → DIAGNOSING → IN_PROGRESS → QC_PENDING → COMPLETED
 * (or → CANCELLED from OPEN/DIAGNOSING/IN_PROGRESS). Completion may re-grade the
 * unit through a fresh technical inspection.
 */
@ApplicationScoped
public class RefurbishmentService {
    private static final Logger log = Logger.getLogger(RefurbishmentService.class);
    private static final String ROLE_ADMIN = "ADMIN";

    private final RepairTicketRepository repairTicketRepository;
    private final ProductUnitRepository productUnitRepository;
    private final InspectionService inspectionService;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public RefurbishmentService(
            RepairTicketRepository repairTicketRepository,
            ProductUnitRepository productUnitRepository,
            InspectionService inspectionService,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.repairTicketRepository = repairTicketRepository;
        this.productUnitRepository = productUnitRepository;
        this.inspectionService = inspectionService;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public RepairDtos.RepairTicketDto createTicket(Long technicianId, RepairDtos.CreateRepairTicketDto request) {
        // Same unit-loading mechanism as InspectionService / OrderFulfillmentService
        ProductUnit unit = productUnitRepository.findByIdOptional(request.unitId())
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        RepairTicket ticket = repairTicketRepository.save(new RepairTicket(
                request.unitId(), technicianId, request.issueDescription(), request.initialPartsCost()));

        auditService.record("RepairTicket", ticket.getId().toString(), "STATE_TRANSITION", technicianId,
                null, null, RepairTicket.Status.OPEN.name());
        emit(ticket, "REPAIR_TICKET_OPENED", "REPAIR_OPENED:" + ticket.getId(),
                new TicketEventPayload(ticket.getId(), ticket.getUnitId(), ticket.getTechnicianId(),
                        ticket.getStatus().name(), ticket.getPartsCost(), null));
        return toDto(ticket);
    }

    @Transactional
    public RepairDtos.RepairTicketDto startDiagnosis(UUID ticketId, Long technicianId, boolean isAdmin) {
        RepairTicket ticket = load(ticketId);
        requireTechnicianOrAdmin(ticket, technicianId, isAdmin);
        requireStatus(ticket, RepairTicket.Status.OPEN, "start diagnosis");

        transition(ticket, technicianId, RepairTicket.Status.DIAGNOSING);
        return toDto(ticket);
    }

    @Transactional
    public RepairDtos.RepairTicketDto startRepair(UUID ticketId, Long technicianId, BigDecimal estimatedPartsCost,
                                                  boolean isAdmin) {
        RepairTicket ticket = load(ticketId);
        requireTechnicianOrAdmin(ticket, technicianId, isAdmin);
        requireStatus(ticket, RepairTicket.Status.DIAGNOSING, "start repair");

        if (estimatedPartsCost != null) {
            if (estimatedPartsCost.signum() < 0) {
                throw new BusinessException("Estimated parts cost cannot be negative", "NEGATIVE_PARTS_COST", 422);
            }
            ticket.setPartsCost(estimatedPartsCost);
        }

        transition(ticket, technicianId, RepairTicket.Status.IN_PROGRESS);
        return toDto(ticket);
    }

    @Transactional
    public RepairDtos.RepairTicketDto submitQc(UUID ticketId, Long technicianId, RepairDtos.SubmitQcDto request,
                                               boolean isAdmin) {
        RepairTicket ticket = load(ticketId);
        requireTechnicianOrAdmin(ticket, technicianId, isAdmin);
        requireStatus(ticket, RepairTicket.Status.IN_PROGRESS, "submit QC");

        BigDecimal total = BigDecimal.ZERO;
        for (RepairDtos.ComponentReplacementDto component : request.components()) {
            if (component.cost() != null && component.cost().signum() < 0) {
                throw new BusinessException("Component cost cannot be negative: " + component.componentName(),
                        "NEGATIVE_COMPONENT_COST", 422);
            }
            if (component.cost() != null) {
                total = total.add(component.cost());
            }
        }
        if (request.totalPartsCost() != null) {
            if (request.totalPartsCost().signum() < 0) {
                throw new BusinessException("Total parts cost cannot be negative", "NEGATIVE_PARTS_COST", 422);
            }
            total = request.totalPartsCost();
        }

        try {
            ticket.setReplacedComponents(objectMapper.writeValueAsString(request.components()));
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize replaced components", "COMPONENT_SERIALIZATION_FAILED", 422);
        }
        ticket.setPartsCost(total);

        transition(ticket, technicianId, RepairTicket.Status.QC_PENDING);
        return toDto(ticket);
    }

    @Transactional
    public RepairDtos.RepairTicketDto completeTicket(UUID ticketId, Long technicianId, RepairDtos.CompleteRepairDto request,
                                                     boolean isAdmin) {
        RepairTicket ticket = load(ticketId);
        requireTechnicianOrAdmin(ticket, technicianId, isAdmin);
        requireStatus(ticket, RepairTicket.Status.QC_PENDING, "complete");

        if (request.regraded()) {
            if (request.newPhysicalScore() == null || request.newHardwareScore() == null
                    || request.newSoftwareScore() == null) {
                throw new BusinessException("Re-grading requires physical, hardware and software scores",
                        "REGRADE_SCORES_REQUIRED", 422);
            }
            // InspectionService.createInspection already writes the certified grade back onto
            // the unit; no trade-in linkage needed (tradeInRequestId stays null).
            inspectionService.createInspection(technicianId, new InspectionDtos.CreateInspectionRequest(
                    ticket.getUnitId(),
                    request.newPhysicalScore(),
                    request.newHardwareScore(),
                    request.newSoftwareScore(),
                    false,
                    null,
                    request.technicianNotes()
            ));
        }

        transition(ticket, technicianId, RepairTicket.Status.COMPLETED);
        emit(ticket, "REPAIR_COMPLETED", "REPAIR_COMPLETED:" + ticket.getId(),
                new TicketEventPayload(ticket.getId(), ticket.getUnitId(), ticket.getTechnicianId(),
                        ticket.getStatus().name(), ticket.getPartsCost(), request.technicianNotes()));
        return toDto(ticket);
    }

    @Transactional
    public RepairDtos.RepairTicketDto cancelTicket(UUID ticketId, Long technicianId, String reason, boolean isAdmin) {
        RepairTicket ticket = load(ticketId);
        requireTechnicianOrAdmin(ticket, technicianId, isAdmin);
        if (ticket.getStatus() == RepairTicket.Status.QC_PENDING) {
            throw new BusinessException("Ticket already submitted for QC cannot be cancelled",
                    "REPAIR_IN_QC_CANNOT_CANCEL", 409);
        }
        if (ticket.getStatus() == RepairTicket.Status.COMPLETED || ticket.getStatus() == RepairTicket.Status.CANCELLED) {
            throw new BusinessException(String.format("Ticket must not be COMPLETED or CANCELLED to cancel (current: %s)",
                    ticket.getStatus()), "INVALID_REPAIR_STATE", 409);
        }

        transition(ticket, technicianId, RepairTicket.Status.CANCELLED);
        emit(ticket, "REPAIR_CANCELLED", "REPAIR_CANCELLED:" + ticket.getId(),
                new TicketEventPayload(ticket.getId(), ticket.getUnitId(), ticket.getTechnicianId(),
                        ticket.getStatus().name(), ticket.getPartsCost(), reason));
        return toDto(ticket);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public RepairDtos.RepairTicketDto getTicket(UUID ticketId, Long callerId, boolean isAdmin) {
        RepairTicket ticket = load(ticketId);
        if (!isAdmin) {
            requireTechnicianOrAdmin(ticket, callerId, false);
        }
        return toDto(ticket);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<RepairDtos.RepairTicketDto> listTickets(RepairTicket.Status status, UUID unitId,
                                                        Long callerId, boolean isAdmin, int page, int size) {
        if (unitId != null) {
            List<RepairTicket> tickets = repairTicketRepository.findByUnitId(unitId);
            return new Page<>(tickets.stream()
                    .filter(t -> isAdmin || t.getTechnicianId().equals(callerId))
                    .filter(t -> status == null || t.getStatus() == status)
                    .map(RefurbishmentService::toDto).toList(),
                    tickets.size(), page, size);
        }
        Page<RepairTicket> result;
        if (status != null) {
            result = repairTicketRepository.findPagedByStatus(status, page, size);
        } else if (isAdmin) {
            result = repairTicketRepository.findPagedAll(page, size);
        } else {
            result = repairTicketRepository.findPagedByTechnicianId(callerId, page, size);
        }
        return new Page<>(result.items().stream().map(RefurbishmentService::toDto).toList(),
                result.total(), page, size);
    }

    private RepairTicket load(UUID ticketId) {
        return repairTicketRepository.findByIdOptional(ticketId)
                .orElseThrow(() -> new BusinessException("Repair ticket not found", "REPAIR_TICKET_NOT_FOUND", 404));
    }

    private void requireTechnicianOrAdmin(RepairTicket ticket, Long callerId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!ticket.getTechnicianId().equals(callerId)) {
            throw new BusinessException("Repair ticket does not belong to this technician", "REPAIR_FORBIDDEN", 403);
        }
    }

    private void requireStatus(RepairTicket ticket, RepairTicket.Status expected, String action) {
        if (ticket.getStatus() != expected) {
            throw new BusinessException(
                    String.format("Repair ticket must be %s to %s (current: %s)",
                            expected, action, ticket.getStatus()),
                    "INVALID_REPAIR_STATE", 409);
        }
    }

    /** Shared transition bookkeeping: save, audit, outbox state event. */
    private void transition(RepairTicket ticket, Long technicianId, RepairTicket.Status toState) {
        RepairTicket.Status fromState = ticket.getStatus();
        ticket.setStatus(toState);
        repairTicketRepository.save(ticket);
        auditService.record("RepairTicket", ticket.getId().toString(), "STATE_TRANSITION", technicianId,
                null, fromState.name(), toState.name());
        emit(ticket, "REPAIR_STATE_CHANGED", "REPAIR_STATE:" + ticket.getId() + ":" + toState,
                new TicketEventPayload(ticket.getId(), ticket.getUnitId(), ticket.getTechnicianId(),
                        toState.name(), ticket.getPartsCost(), null));
    }

    private void emit(RepairTicket ticket, String eventType, String idempotencyKey, TicketEventPayload payload) {
        try {
            outboxEventRepository.save(new OutboxEvent(
                    "REPAIR",
                    ticket.getId().toString(),
                    eventType,
                    objectMapper.writeValueAsString(payload),
                    instanceCorrelationId,
                    idempotencyKey
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize %s payload for repair ticket %s", eventType, ticket.getId());
        }
    }

    public static RepairDtos.RepairTicketDto toDto(RepairTicket t) {
        return new RepairDtos.RepairTicketDto(
                t.getId(),
                t.getUnitId(),
                t.getTechnicianId(),
                t.getIssueDescription(),
                t.getReplacedComponents(),
                t.getPartsCost(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    record TicketEventPayload(
            UUID ticketId,
            UUID unitId,
            Long technicianId,
            String status,
            BigDecimal partsCost,
            String notes
    ) {}
}
