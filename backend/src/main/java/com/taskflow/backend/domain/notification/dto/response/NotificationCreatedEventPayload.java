package com.taskflow.backend.domain.notification.dto.response;

import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;

public record NotificationCreatedEventPayload(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        NotificationReferenceType referenceType,
        Long referenceId
) {
}
