package com.reloop.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import com.reloop.notifications.domain.Notification;
import com.reloop.notifications.dto.NotificationDtos;
import com.reloop.notifications.repository.NotificationRepository;
import com.reloop.outbox.domain.OutboxEvent;
import com.reloop.outbox.repository.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * In-app notification center. push() writes the in-app row and best-effort
 * mirrors it as an EMAIL outbox event ({to, subject, body}) for the existing
 * OutboxEventProcessor email dispatch path.
 */
@ApplicationScoped
public class NotificationService {
    private static final Logger log = Logger.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final UUID instanceCorrelationId;

    @Inject
    public NotificationService(
            NotificationRepository notificationRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.instanceCorrelationId = UUID.randomUUID();
    }

    @Transactional
    public NotificationDtos.NotificationResponse push(Long recipientId, Notification.Category category,
                                                        String title, String body,
                                                        String referenceType, UUID referenceId) {
        Notification notification = notificationRepository.save(
                new Notification(recipientId, category, title, body, referenceType, referenceId));

        String email = notificationRepository.resolveEmail(recipientId);
        if (email == null || email.isBlank()) {
            log.warnf("No email for recipient %s; in-app notification %s only", recipientId, notification.getId());
        } else {
            try {
                String payload = objectMapper.writeValueAsString(new EmailPayload(email, title, body));
                outboxEventRepository.save(new OutboxEvent(
                        "EMAIL",
                        notification.getId().toString(),
                        "EMAIL_NOTIFICATION",
                        payload,
                        instanceCorrelationId,
                        "EMAIL_NOTIFICATION:" + notification.getId()
                ));
            } catch (JsonProcessingException e) {
                log.errorf(e, "Failed to serialize email payload for notification %s", notification.getId());
            }
        }
        return toResponse(notification);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public NotificationDtos.NotificationsPageResponse myNotifications(Long userId, int page, int size, Boolean unreadOnly) {
        var query = unreadOnly != null && unreadOnly
                ? notificationRepository.find("recipientId = ?1 AND isRead = false",
                        io.quarkus.panache.common.Sort.descending("createdAt"), userId)
                : notificationRepository.find("recipientId = ?1",
                        io.quarkus.panache.common.Sort.descending("createdAt"), userId);
        long total = query.count();
        List<Notification> items = query.page(io.quarkus.panache.common.Page.of(page, size)).list();
        long unreadCount = notificationRepository.countUnread(userId);
        return new NotificationDtos.NotificationsPageResponse(
                items.stream().map(NotificationService::toResponse).toList(), total, page, size, unreadCount);
    }

    @Transactional
    public void markRead(UUID notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdOptional(notificationId)
                .orElseThrow(() -> new BusinessException("Notification not found", "NOTIFICATION_NOT_FOUND", 404));
        if (!notification.getRecipientId().equals(userId)) {
            throw new BusinessException("Notification not found", "NOTIFICATION_NOT_FOUND", 404);
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    static NotificationDtos.NotificationResponse toResponse(Notification n) {
        return new NotificationDtos.NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getTitle(),
                n.getBody(),
                n.getCategory().name(),
                n.getReferenceType(),
                n.getReferenceId(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }

    record EmailPayload(String to, String subject, String body) {}
}
