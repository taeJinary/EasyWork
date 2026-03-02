package com.taskflow.backend.domain.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationReadResponse(
        Long notificationId,
        boolean isRead,
        LocalDateTime readAt
) {
}
