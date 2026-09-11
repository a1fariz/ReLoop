package com.reloop.notifications.repository;

import com.reloop.common.jpa.ReloopRepository;
import com.reloop.notifications.domain.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

@ApplicationScoped
public class NotificationRepository implements ReloopRepository<Notification, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    public long countUnread(Long recipientId) {
        return count("recipientId = ?1 AND isRead = false", recipientId);
    }

    /** Bulk-reads all unread notifications for a recipient. */
    public int markAllRead(Long recipientId) {
        return update("isRead = true, readAt = current_timestamp WHERE recipientId = ?1 AND isRead = false", recipientId);
    }

    /**
     * users.email lookup via native query (mirrors
     * FulfillmentOrderRepository.resolveSellerUserId) to keep the module
     * boundary: no dependency on the auth module's repository bean.
     */
    public String resolveEmail(Long userId) {
        Object result = entityManager
                .createNativeQuery("SELECT email FROM users WHERE id = ?1")
                .setParameter(1, userId)
                .getResultList().stream().findFirst().orElse(null);
        return result != null ? result.toString() : null;
    }
}
