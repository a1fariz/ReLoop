package com.reloop.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.notifications.domain.Notification;
import com.reloop.notifications.repository.NotificationRepository;
import com.reloop.notifications.service.NotificationService;
import com.reloop.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                outboxEventRepository,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("push creates the in-app notification and mirrors it as an EMAIL outbox event")
    void testPushCreatesRowAndEmailEvent() {
        UUID referenceId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            com.reloop.support.TestFields.set(n, "id", UUID.randomUUID());
            return n;
        });
        when(notificationRepository.resolveEmail(10L)).thenReturn("buyer@reloop.id");

        var response = notificationService.push(10L, Notification.Category.RETURN,
                "Return REFUNDED", "Return refunded: IDR 1000.00", "RETURN", referenceId);

        assertThat(response.recipientId()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo("RETURN");
        assertThat(response.isRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("push still creates the in-app row when the recipient email is unavailable")
    void testPushWithoutEmailSkipsOutbox() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            com.reloop.support.TestFields.set(n, "id", UUID.randomUUID());
            return n;
        });
        when(notificationRepository.resolveEmail(10L)).thenReturn(null);

        var response = notificationService.push(10L, Notification.Category.ORDER,
                "Order shipped", "on the way", "ORDER", null);

        assertThat(response.recipientId()).isEqualTo(10L);
        verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("markRead flips the read flag on an owned notification")
    void testMarkReadFlipsFlag() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(10L, Notification.Category.ORDER,
                "Order shipped", "Your order is on the way", "ORDER", null);
        com.reloop.support.TestFields.set(notification, "id", notificationId);

        when(notificationRepository.findByIdOptional(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.markRead(notificationId, 10L);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markRead by a non-recipient is a silent 404 (IDOR guard)")
    void testMarkReadUnauthorizedThrows() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(10L, Notification.Category.ORDER,
                "Order shipped", "Your order is on the way", "ORDER", null);
        com.reloop.support.TestFields.set(notification, "id", notificationId);

        when(notificationRepository.findByIdOptional(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(notificationId, 99L))
                .isInstanceOf(com.reloop.common.exception.BusinessException.class)
                .hasMessageContaining("not found");
    }
}
