package com.reloop.notifications.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NotificationDtos {
    public record NotificationResponse(
            UUID id,
            Long recipientId,
            String title,
            String body,
            String category,
            String referenceType,
            UUID referenceId,
            boolean isRead,
            Instant readAt,
            Instant createdAt
    ) {}

    public record NotificationsPageResponse(
            List<NotificationResponse> items,
            long total,
            int page,
            int size,
            long unreadCount
    ) {}
}
