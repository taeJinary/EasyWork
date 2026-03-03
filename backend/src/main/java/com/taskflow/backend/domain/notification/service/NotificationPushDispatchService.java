package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPushDispatchService {

    private final NotificationPushTokenRepository notificationPushTokenRepository;
    private final NotificationPushSender notificationPushSender;

    public void send(Notification notification) {
        List<NotificationPushToken> tokens =
                notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(notification.getUser().getId());

        for (NotificationPushToken token : tokens) {
            try {
                notificationPushSender.send(
                        token.getToken(),
                        token.getPlatform(),
                        notification.getTitle(),
                        notification.getContent()
                );
            } catch (Exception exception) {
                log.warn(
                        "Failed to send push notification. notificationId={}, userId={}, token={}",
                        notification.getId(),
                        notification.getUser().getId(),
                        token.getToken(),
                        exception
                );
            }
        }
    }
}
