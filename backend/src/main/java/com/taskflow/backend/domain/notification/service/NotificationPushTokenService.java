package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.dto.response.NotificationPushTokenResponse;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPushTokenService {

    private final NotificationPushTokenRepository notificationPushTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public NotificationPushTokenResponse registerPushToken(Long userId, String token, PushPlatform platform) {
        User user = findActiveUser(userId);

        NotificationPushToken pushToken = notificationPushTokenRepository.findByToken(token)
                .map(existing -> {
                    existing.reactivate(user, platform);
                    return existing;
                })
                .orElseGet(() -> notificationPushTokenRepository.save(
                        NotificationPushToken.create(user, token, platform)
                ));

        return toResponse(pushToken);
    }

    @Transactional
    public boolean unregisterPushToken(Long userId, String token) {
        findActiveUser(userId);

        return notificationPushTokenRepository.findByUserIdAndToken(userId, token)
                .map(pushToken -> {
                    pushToken.deactivate();
                    notificationPushTokenRepository.save(pushToken);
                    return true;
                })
                .orElse(false);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private NotificationPushTokenResponse toResponse(NotificationPushToken pushToken) {
        return new NotificationPushTokenResponse(
                pushToken.getToken(),
                pushToken.getPlatform(),
                pushToken.isActive()
        );
    }
}
