package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.logging.SensitiveValueSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.notification.push.sender", havingValue = "logging", matchIfMissing = true)
public class LoggingNotificationPushSender implements NotificationPushSender {

    @Override
    public void send(String token, PushPlatform platform, String title, String body) {
        log.debug(
                "Push notification send requested. platform={}, tokenHash={}, title={}",
                platform,
                tokenHash(token),
                title
        );
    }

    String tokenHash(String token) {
        return SensitiveValueSanitizer.shortHash(token);
    }
}
