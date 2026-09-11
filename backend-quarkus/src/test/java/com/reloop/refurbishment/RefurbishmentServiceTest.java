package com.reloop.refurbishment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import com.reloop.inspections.service.InspectionService;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.refurbishment.domain.RepairTicket;
import com.reloop.refurbishment.dto.RepairDtos;
import com.reloop.refurbishment.repository.RepairTicketRepository;
import com.reloop.refurbishment.service.RefurbishmentService;
import com.reloop.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefurbishmentServiceTest {

    @Mock
    private RepairTicketRepository repairTicketRepository;
    @Mock
    private InspectionService inspectionService;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AuditService auditService;

    private RefurbishmentService refurbishmentService;

    private static final Long TECHNICIAN = 42L;
    private static final UUID UNIT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        refurbishmentService = new RefurbishmentService(
                repairTicketRepository,
                null,
                inspectionService,
                outboxEventRepository,
                auditService,
                new ObjectMapper()
        );
    }

    private RepairTicket newTicket(RepairTicket.Status status, UUID ticketId) {
        RepairTicket ticket = new RepairTicket(UNIT_ID, TECHNICIAN, "Broken charging port", BigDecimal.ZERO);
        ticket.setStatus(status);
        com.reloop.support.TestFields.set(ticket, "id", ticketId);
        return ticket;
    }

    private void stubLoad(RepairTicket ticket) {
        lenient().when(repairTicketRepository.findByIdOptional(ticket.getId())).thenReturn(Optional.of(ticket));
        lenient().when(repairTicketRepository.save(any(RepairTicket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Happy path: OPEN → DIAGNOSING → IN_PROGRESS → QC_PENDING → COMPLETED with components and parts cost")
    void testFullLifecycleHappyPath() {
        RepairTicket ticket = newTicket(RepairTicket.Status.OPEN, UUID.randomUUID());
        stubLoad(ticket);

        var dto = refurbishmentService.startDiagnosis(ticket.getId(), TECHNICIAN, false);
        assertThat(dto.status()).isEqualTo("DIAGNOSING");

        dto = refurbishmentService.startRepair(ticket.getId(), TECHNICIAN, new BigDecimal("250000.00"), false);
        assertThat(dto.status()).isEqualTo("IN_PROGRESS");

        var component = new RepairDtos.ComponentReplacementDto("Charging port flex", "CPF-9A1", new BigDecimal("180000.00"));
        dto = refurbishmentService.submitQc(ticket.getId(), TECHNICIAN,
                new RepairDtos.SubmitQcDto(List.of(component), null), false);
        assertThat(dto.status()).isEqualTo("QC_PENDING");
        assertThat(dto.partsCost()).isEqualByComparingTo(new BigDecimal("180000.00"));
        assertThat(dto.replacedComponents()).contains("Charging port flex");

        dto = refurbishmentService.completeTicket(ticket.getId(), TECHNICIAN,
                new RepairDtos.CompleteRepairDto(true, 90, 95, 100, "Replaced port, regraded"), false);
        assertThat(dto.status()).isEqualTo("COMPLETED");
        verify(inspectionService).createInspection(any(), any());
    }

    @Test
    @DisplayName("Cancel from QC_PENDING is rejected with 409 REPAIR_IN_QC_CANNOT_CANCEL")
    void testCancelFromQcPendingRejected() {
        RepairTicket ticket = newTicket(RepairTicket.Status.QC_PENDING, UUID.randomUUID());
        stubLoad(ticket);

        assertThatThrownBy(() -> refurbishmentService.cancelTicket(ticket.getId(), TECHNICIAN, "wrong part shipped", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    @DisplayName("Cancel from OPEN is allowed and lands on CANCELLED")
    void testCancelFromOpenAllowed() {
        RepairTicket ticket = newTicket(RepairTicket.Status.OPEN, UUID.randomUUID());
        stubLoad(ticket);

        var dto = refurbishmentService.cancelTicket(ticket.getId(), TECHNICIAN, "duplicate ticket", false);
        assertThat(dto.status()).isEqualTo("CANCELLED");
        verify(inspectionService, never()).createInspection(any(), any());
    }
}
