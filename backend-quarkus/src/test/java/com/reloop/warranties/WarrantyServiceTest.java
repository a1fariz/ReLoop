package com.reloop.warranties;

import com.reloop.warranties.domain.Warranty;
import com.reloop.warranties.repository.WarrantyRepository;
import com.reloop.warranties.service.WarrantyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantyServiceTest {

    @Mock
    private WarrantyRepository warrantyRepository;

    private WarrantyService warrantyService;

    @BeforeEach
    void setUp() {
        warrantyService = new WarrantyService(warrantyRepository);
    }

    @Test
    @DisplayName("Issues a standard 6-month warranty for the buyer when none exists")
    void testIssueCreatesWarranty() {
        UUID unitId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        when(warrantyRepository.findByUnitIdAndIsVoidedFalse(unitId)).thenReturn(Optional.empty());
        when(warrantyRepository.save(any(Warranty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warranty warranty = warrantyService.issueStandardWarranty(unitId, 10L, fulfillmentId);

        assertThat(warranty.getOwnerId()).isEqualTo(10L);
        assertThat(warranty.getPolicyTier()).isEqualTo("STANDARD_6_MONTHS");
        assertThat(warranty.isVoided()).isFalse();
        assertThat(warranty.getExpiresAt()).isAfter(Instant.now().plus(180, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("Warranty issuance is idempotent: existing policy is returned unchanged")
    void testIssueIdempotent() {
        UUID unitId = UUID.randomUUID();
        Warranty existing = new Warranty(unitId, 10L, UUID.randomUUID(),
                Instant.now(), Instant.now().plus(182, ChronoUnit.DAYS), "STANDARD_6_MONTHS");
        when(warrantyRepository.findByUnitIdAndIsVoidedFalse(unitId)).thenReturn(Optional.of(existing));

        Warranty result = warrantyService.issueStandardWarranty(unitId, 10L, UUID.randomUUID());

        assertThat(result).isSameAs(existing);
        verify(warrantyRepository, never()).save(any());
    }
}
