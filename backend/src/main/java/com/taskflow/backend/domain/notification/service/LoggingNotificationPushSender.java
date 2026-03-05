package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.common.enums.PushPlatform;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        if (!StringUtils.hasText(token)) {
            return "empty";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }
}
