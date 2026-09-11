package com.reloop.users.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.audit.service.AuditService;
import com.reloop.common.exception.BusinessException;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import com.reloop.users.domain.UserKycLog;
import com.reloop.users.dto.UserDtos;
import com.reloop.users.repository.UserKycLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Manages KYC state and public profile fields on the users table via native
 * SQL, because the users table is owned by the auth module's User entity and
 * cross-module JPA access is prohibited. KYC transitions are recorded in the
 * users module's own user_kyc_logs projection.
 */
@ApplicationScoped
public class UserKycService {
    private static final Logger log = Logger.getLogger(UserKycService.class);

    private final UserKycLogRepository kycLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    public UserKycService(
            UserKycLogRepository kycLogRepository,
            OutboxEventRepository outboxEventRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.kycLogRepository = kycLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public void submitKyc(Long userId, UserDtos.KycSubmissionDto request) {
        requireUserExists(userId);
        String current = kycStatus(userId);
        if (!"PENDING".equals(current) && !"REJECTED".equals(current)) {
            throw new BusinessException("KYC has already been submitted", "KYC_ALREADY_SUBMITTED", 409);
        }

        Instant now = Instant.now();
        entityManager.createNativeQuery("""
                        UPDATE users
                        SET kyc_status = 'SUBMITTED',
                            kyc_document_type = :documentType,
                            kyc_document_reference = :documentReference,
                            national_id = :nationalId,
                            date_of_birth = :dateOfBirth,
                            kyc_submitted_at = :submittedAt,
                            kyc_verified_at = NULL,
                            updated_at = NOW()
                        WHERE id = :userId
                        """)
                .setParameter("documentType", request.documentType())
                .setParameter("documentReference", request.documentReference())
                .setParameter("nationalId", request.nationalId())
                .setParameter("dateOfBirth", java.sql.Date.valueOf(request.dateOfBirth()))
                .setParameter("submittedAt", Timestamp.from(now))
                .setParameter("userId", userId)
                .executeUpdate();

        kycLogRepository.save(new UserKycLog(userId, current, "SUBMITTED", null, null));
        auditService.record("USER", userId.toString(), "KYC_SUBMITTED", userId, null, current, "SUBMITTED");
        emitKycEvent(userId, "KYC_SUBMITTED", "KYC_SUBMITTED:" + userId + ":" + now.getEpochSecond());
    }

    @Transactional
    public void reviewKyc(Long targetUserId, Long adminId, UserDtos.KycReviewDto request) {
        requireUserExists(targetUserId);
        String current = kycStatus(targetUserId);
        if (!"SUBMITTED".equals(current)) {
            throw new BusinessException("KYC has not been submitted for review", "KYC_NOT_SUBMITTED", 409);
        }

        boolean approved = Boolean.TRUE.equals(request.approved());
        String newStatus = approved ? "VERIFIED" : "REJECTED";
        entityManager.createNativeQuery("""
                        UPDATE users
                        SET kyc_status = :newStatus,
                            kyc_verified_at = :verifiedAt,
                            is_verified = :isVerified,
                            updated_at = NOW()
                        WHERE id = :userId
                        """)
                .setParameter("newStatus", newStatus)
                .setParameter("verifiedAt", approved ? Timestamp.from(Instant.now()) : null)
                .setParameter("isVerified", approved)
                .setParameter("userId", targetUserId)
                .executeUpdate();

        kycLogRepository.save(new UserKycLog(targetUserId, current, newStatus, adminId, request.notes()));
        auditService.record("USER", targetUserId.toString(), "KYC_" + newStatus, adminId, null, current, newStatus);
        emitKycEvent(targetUserId, "KYC_" + newStatus, "KYC_" + newStatus + ":" + targetUserId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public UserDtos.KycStatusDto getKycStatus(Long userId) {
        requireUserExists(userId);
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT kyc_status, kyc_document_type, kyc_document_reference,
                               national_id, date_of_birth, kyc_submitted_at, kyc_verified_at
                        FROM users WHERE id = :userId
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        return new UserDtos.KycStatusDto(
                userId,
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                asLocalDate(row[4]),
                asInstant(row[5]),
                asInstant(row[6])
        );
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public UserDtos.ProfileDto getProfile(Long userId) {
        requireUserExists(userId);
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT email, full_name, phone_number, role, is_verified, kyc_status, address
                        FROM users WHERE id = :userId
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        return new UserDtos.ProfileDto(
                userId,
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                asBoolean(row[4]),
                (String) row[5],
                row[6] != null ? row[6].toString() : null
        );
    }

    @Transactional
    public void updateProfile(Long userId, UserDtos.UpdateProfileDto request) {
        requireUserExists(userId);
        String address = request.address() != null && !request.address().isBlank()
                ? request.address() : "{}";
        try {
            objectMapper.readTree(address);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Address must be valid JSON", "INVALID_ADDRESS", 400);
        }

        entityManager.createNativeQuery("""
                        UPDATE users
                        SET full_name = :fullName,
                            phone_number = :phoneNumber,
                            address = CAST(:address AS jsonb),
                            updated_at = NOW()
                        WHERE id = :userId
                        """)
                .setParameter("fullName", request.fullName())
                .setParameter("phoneNumber", request.phoneNumber())
                .setParameter("address", address)
                .setParameter("userId", userId)
                .executeUpdate();

        auditService.record("USER", userId.toString(), "PROFILE_UPDATED", userId, null, null, "UPDATED");
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<UserDtos.KycLogDto> getKycLogs(Long userId, int page, int size) {
        var query = kycLogRepository.findByUserIdPaged(userId, page, size);
        long total = query.count(); // count() ignores paging: total across all pages
        List<UserKycLog> items = query.list();
        return new com.reloop.common.dto.Page<>(
                items.stream().map(UserKycService::toLogDto).toList(), total, page, size);
    }

    private void requireUserExists(Long userId) {
        Object count = entityManager.createNativeQuery("SELECT COUNT(*) FROM users WHERE id = :userId")
                .setParameter("userId", userId)
                .getSingleResult();
        if (((Number) count).longValue() == 0) {
            throw new BusinessException("User not found", "USER_NOT_FOUND", 404);
        }
    }

    private String kycStatus(Long userId) {
        return (String) entityManager.createNativeQuery("SELECT kyc_status FROM users WHERE id = :userId")
                .setParameter("userId", userId)
                .getSingleResult();
    }

    /** Native queries hand back Timestamp/LocalDateTime/OffsetDateTime depending on driver/Hibernate: normalize. */
    private static Instant asInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toInstant();
        if (value instanceof OffsetDateTime odt) return odt.toInstant();
        if (value instanceof LocalDateTime ldt) return ldt.toInstant(ZoneOffset.UTC);
        if (value instanceof Instant i) return i;
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
    }

    private static LocalDate asLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof LocalDate ld) return ld;
        throw new IllegalStateException("Unsupported date type: " + value.getClass());
    }

    private static boolean asBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private void emitKycEvent(Long userId, String eventType, String idempotencyKey) {
        try {
            String payload = objectMapper.writeValueAsString(new KycEventPayload(userId, eventType));
            outboxEventRepository.save(new OutboxEvent(
                    "USER",
                    userId.toString(),
                    eventType,
                    payload,
                    instanceCorrelationId,
                    idempotencyKey
            ));
        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize %s payload for user %s", eventType, userId);
        }
    }

    private static UserDtos.KycLogDto toLogDto(UserKycLog l) {
        return new UserDtos.KycLogDto(
                l.getId(),
                l.getUserId(),
                l.getPreviousStatus(),
                l.getNewStatus(),
                l.getReviewedBy(),
                l.getNotes(),
                l.getCreatedAt()
        );
    }

    record KycEventPayload(Long userId, String eventType) {}
}
