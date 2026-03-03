package com.taskflow.backend.domain.notification.dto.request;

import com.taskflow.backend.global.common.enums.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterNotificationPushTokenRequest(
        @NotBlank
        @Size(max = 512)
        String token,

        @NotNull
        PushPlatform platform
) {
}
