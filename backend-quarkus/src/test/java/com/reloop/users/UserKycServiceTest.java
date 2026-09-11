package com.reloop.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.users.domain.UserKycLog;
import com.reloop.users.dto.UserDtos;
import com.reloop.users.repository.UserKycLogRepository;
import com.reloop.users.service.UserKycService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserKycServiceTest {

    @Mock
    private UserKycLogRepository kycLogRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query countQuery;
    @Mock
    private Query statusQuery;
    @Mock
    private Query updateQuery;

    private UserKycService userKycService;

    @BeforeEach
    void setUp() {
        userKycService = new UserKycService(kycLogRepository, outboxEventRepository, auditService, new ObjectMapper());
        com.reloop.support.TestFields.set(userKycService, "entityManager", entityManager);
    }

    /** Stubs the user existence check + current kyc_status read. */
    private void stubUser(String kycStatus) {
        lenient().when(entityManager.createNativeQuery(contains("COUNT(*)"))).thenReturn(countQuery);
        lenient().when(entityManager.createNativeQuery(contains("kyc_status FROM users"))).thenReturn(statusQuery);
        lenient().when(entityManager.createNativeQuery(contains("UPDATE users"))).thenReturn(updateQuery);
        lenient().when(countQuery.setParameter(any(String.class), any())).thenReturn(countQuery);
        lenient().when(statusQuery.setParameter(any(String.class), any())).thenReturn(statusQuery);
        lenient().when(updateQuery.setParameter(any(String.class), any())).thenReturn(updateQuery);
        lenient().when(countQuery.getSingleResult()).thenReturn(BigInteger.ONE);
        lenient().when(statusQuery.getSingleResult()).thenReturn(kycStatus);
    }

    private UserDtos.KycSubmissionDto submission() {
        return new UserDtos.KycSubmissionDto("KTP", "KTP-1234-5678", "3273010101990001", LocalDate.of(1999, 1, 1));
    }

    @Test
    @DisplayName("Submit KYC from PENDING: logs transition, audit entry and outbox event")
    void testSubmitKyc() {
        stubUser("PENDING");

        userKycService.submitKyc(10L, submission());

        var logCaptor = ArgumentCaptor.forClass(UserKycLog.class);
        verify(kycLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(logCaptor.getValue().getPreviousStatus()).isEqualTo("PENDING");
        assertThat(logCaptor.getValue().getNewStatus()).isEqualTo("SUBMITTED");
        verify(auditService).record(eq("USER"), eq("10"), eq("KYC_SUBMITTED"), eq(10L), any(), eq("PENDING"), eq("SUBMITTED"));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("KYC can be resubmitted after REJECTED")
    void testResubmitAfterRejection() {
        stubUser("REJECTED");

        userKycService.submitKyc(10L, submission());

        verify(kycLogRepository).save(any(UserKycLog.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Resubmitting KYC while SUBMITTED or VERIFIED is blocked (409)")
    void testRejectDoubleSubmission() {
        stubUser("SUBMITTED");

        assertThatThrownBy(() -> userKycService.submitKyc(10L, submission()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been submitted");
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin approval: VERIFIED transition logged with reviewer and outbox event")
    void testApproveKyc() {
        stubUser("SUBMITTED");

        userKycService.reviewKyc(10L, 999L, new UserDtos.KycReviewDto(Boolean.TRUE, "Documents match"));

        var logCaptor = ArgumentCaptor.forClass(UserKycLog.class);
        verify(kycLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getNewStatus()).isEqualTo("VERIFIED");
        assertThat(logCaptor.getValue().getReviewedBy()).isEqualTo(999L);
        assertThat(logCaptor.getValue().getNotes()).isEqualTo("Documents match");
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Admin rejection: REJECTED transition logged")
    void testRejectKyc() {
        stubUser("SUBMITTED");

        userKycService.reviewKyc(10L, 999L, new UserDtos.KycReviewDto(Boolean.FALSE, "Blurry document"));

        var logCaptor = ArgumentCaptor.forClass(UserKycLog.class);
        verify(kycLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getNewStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Review of a user whose KYC was never submitted is blocked (409)")
    void testRejectReviewWithoutSubmission() {
        stubUser("PENDING");

        assertThatThrownBy(() -> userKycService.reviewKyc(10L, 999L, new UserDtos.KycReviewDto(Boolean.TRUE, "no")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not been submitted");
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unknown user: KYC submit, review and profile reads all 404")
    void testUnknownUser() {
        when(entityManager.createNativeQuery(contains("COUNT(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(any(String.class), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(BigInteger.ZERO);

        assertThatThrownBy(() -> userKycService.submitKyc(42L, submission()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
        assertThatThrownBy(() -> userKycService.reviewKyc(42L, 999L, new UserDtos.KycReviewDto(Boolean.TRUE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
        assertThatThrownBy(() -> userKycService.getKycStatus(42L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
        assertThatThrownBy(() -> userKycService.getProfile(42L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("updateProfile rejects a non-JSON address (400)")
    void testRejectInvalidAddressJson() {
        stubUser("PENDING");

        assertThatThrownBy(() -> userKycService.updateProfile(10L,
                new UserDtos.UpdateProfileDto("Budi Santoso", "+628123456789", "not-json{{")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("valid JSON");
        verify(updateQuery, never()).executeUpdate();
    }
}
