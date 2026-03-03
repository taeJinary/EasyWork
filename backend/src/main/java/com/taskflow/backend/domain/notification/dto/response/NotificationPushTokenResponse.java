package com.taskflow.backend.domain.notification.dto.response;

import com.taskflow.backend.global.common.enums.PushPlatform;

public record NotificationPushTokenResponse(
        String token,
        PushPlatform platform,
        boolean active
) {
}
