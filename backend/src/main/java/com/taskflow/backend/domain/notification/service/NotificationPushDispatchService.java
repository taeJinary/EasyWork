package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPushDispatchService {

    private final NotificationPushTokenRepository notificationPushTokenRepository;
    private final NotificationPushSender notificationPushSender;

    public NotificationPushDispatchResult send(Notification notification) {
        List<NotificationPushToken> tokens =
                notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(notification.getUser().getId());
        Set<Long> transientFailedTokenIds = new LinkedHashSet<>();

        for (NotificationPushToken token : tokens) {
            if (sendToToken(notification, token) && token.getId() != null) {
                transientFailedTokenIds.add(token.getId());
            }
        }

        return new NotificationPushDispatchResult(Set.copyOf(transientFailedTokenIds));
    }

    boolean sendToToken(Notification notification, NotificationPushToken token) {
        try {
            notificationPushSender.send(
                    token.getToken(),
                    token.getPlatform(),
                    notification.getTitle(),
                    notification.getContent()
            );
            return false;
        } catch (PushTokenInvalidException exception) {
            token.deactivate();
            notificationPushTokenRepository.save(token);
            log.warn(
                    "Push token deactivated after permanent delivery failure. notificationId={}, userId={}, tokenHash={}, reason={}",
                    notification.getId(),
                    notification.getUser().getId(),
                    tokenHash(token.getToken()),
                    exception.getMessage()
            );
            return false;
        } catch (Exception exception) {
            log.warn(
                    "Failed to send push notification. notificationId={}, userId={}, tokenHash={}",
                    notification.getId(),
                    notification.getUser().getId(),
                    tokenHash(token.getToken()),
                    exception
            );
            return true;
        }
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

    public record NotificationPushDispatchResult(Set<Long> transientFailedTokenIds) {
    }
}
