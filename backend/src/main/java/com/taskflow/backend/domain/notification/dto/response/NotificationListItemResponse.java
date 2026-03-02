package com.taskflow.backend.domain.notification.dto.response;

import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import java.time.LocalDateTime;

public record NotificationListItemResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        NotificationReferenceType referenceType,
        Long referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {
}
