package com.reloop.tradein;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.tradein.dto.TradeInDtos;
import com.reloop.tradein.repository.TradeInRequestRepository;
import com.reloop.tradein.service.TradeInService;
import com.reloop.tradein.service.TradeInValuationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeInServiceTest {

    @Mock
    private TradeInRequestRepository tradeInRequestRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private TradeInService tradeInService;

    @BeforeEach
    void setUp() {
        tradeInService = new TradeInService(tradeInRequestRepository, new TradeInValuationService(),
                outboxEventRepository, new ObjectMapper(),
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    private TradeInDtos.SubmitTradeInRequest request(String condition) {
        return new TradeInDtos.SubmitTradeInRequest(
                java.util.UUID.randomUUID(),
                new BigDecimal("10000000.00"),
                new BigDecimal("0.150"),
                LocalDate.now().minusYears(1),
                condition,
                "FULLY_FUNCTIONAL",
                95,
                true,
                BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Offer is recomputed server-side; request saved as SUBMITTED with outbox event")
    void testSubmitComputesAuthoritativeOffer() {
        when(tradeInRequestRepository.save(any())).thenAnswer(invocation -> {
            com.reloop.tradein.domain.TradeInRequest r = invocation.getArgument(0);
            com.reloop.support.TestFields.set(r, "id", java.util.UUID.randomUUID());
            return r;
        });

        var response = tradeInService.submitTradeIn(1L, request("EXCELLENT"));

        assertThat(response.status()).isEqualTo("SUBMITTED");
        // same envelope as the calculate endpoint: ~7,069,662 for these inputs
        assertThat(response.estimatedOffer()).isBetween(new BigDecimal("7000000.00"), new BigDecimal("7200000.00"));
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Unknown condition is rejected with 400")
    void testSubmitInvalidCondition() {
        assertThatThrownBy(() -> tradeInService.submitTradeIn(1L, request("BRAND_NEW_LITERALLY")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unknown condition");
    }
}
