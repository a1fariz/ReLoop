package com.reloop.notifications.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.notifications.dto.NotificationDtos;
import com.reloop.notifications.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public NotificationController(NotificationService notificationService, CurrentUser currentUser,
                                  CorrelationContext correlationContext) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/my")
    public ApiResponse<NotificationDtos.NotificationsPageResponse> getMyNotifications(
            @QueryParam("unreadOnly") Boolean unreadOnly,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(notificationService.myNotifications(currentUser.id(), Math.max(page, 0), safeSize, unreadOnly),
                correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/read")
    public ApiResponse<Void> markRead(@PathParam("id") UUID id) {
        notificationService.markRead(id, currentUser.id());
        return ApiResponse.ok(null, "Notification marked as read", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(currentUser.id());
        return ApiResponse.ok(null, "All notifications marked as read", correlationContext.getCorrelationId());
    }
}
