package com.reloop.disputes;

import com.reloop.common.exception.BusinessException;
import com.reloop.disputes.domain.Dispute;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.repository.DisputeRepository;
import com.reloop.disputes.service.DisputeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @InjectMocks
    private DisputeService disputeService;

    @Test
    @DisplayName("Successfully resolve dispute with partial arbitrated split settlement")
    void testPartialRefundResolution() {
        UUID disputeId = UUID.randomUUID();
        Dispute dispute = new Dispute(UUID.randomUUID(), 10L, 20L, "DAMAGED_SCREEN", "Screen has unlisted scratches");

        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new DisputeDtos.ResolveDisputeRequest(
                "PARTIAL_REFUND",
                new BigDecimal("500000.00"),  // 500k refund to buyer
                new BigDecimal("14500000.00"), // 14.5M net release to seller
                "Agreed on partial compensation for cosmetic scratch"
        );

        var response = disputeService.resolveDispute(disputeId, 999L, request);

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(response.resolutionType()).isEqualTo("PARTIAL_REFUND");
        assertThat(response.buyerRefundAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(response.sellerPayoutAmount()).isEqualByComparingTo(new BigDecimal("14500000.00"));
    }

    @Test
    @DisplayName("Reject resolving an already resolved dispute")
    void testRejectAlreadyResolvedDispute() {
        UUID disputeId = UUID.randomUUID();
        Dispute dispute = new Dispute(UUID.randomUUID(), 10L, 20L, "DAMAGED", "details");
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);

        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

        var request = new DisputeDtos.ResolveDisputeRequest("FULL_REFUND", new BigDecimal("1000.00"), BigDecimal.ZERO, "notes");

        assertThatThrownBy(() -> disputeService.resolveDispute(disputeId, 999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already resolved");
    }
}
