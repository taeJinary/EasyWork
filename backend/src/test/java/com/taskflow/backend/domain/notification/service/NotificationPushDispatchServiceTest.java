package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPushDispatchServiceTest {

    @Mock
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Mock
    private NotificationPushSender notificationPushSender;

    @InjectMocks
    private NotificationPushDispatchService notificationPushDispatchService;

    @Test
    void sendDispatchesToAllActiveTokens() {
        User user = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );

        NotificationPushToken web = NotificationPushToken.create(user, "token-web", PushPlatform.WEB);
        NotificationPushToken android = NotificationPushToken.create(user, "token-android", PushPlatform.ANDROID);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(web, android));

        notificationPushDispatchService.send(notification);

        verify(notificationPushSender).send(
                eq("token-web"),
                eq(PushPlatform.WEB),
                eq("Project invitation"),
                eq("owner invited you")
        );
        verify(notificationPushSender).send(
                eq("token-android"),
                eq(PushPlatform.ANDROID),
                eq("Project invitation"),
                eq("owner invited you")
        );
    }

    @Test
    void sendContinuesWhenSingleTokenSendFails() {
        User user = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );

        NotificationPushToken token1 = NotificationPushToken.create(user, "token-1", PushPlatform.WEB);
        NotificationPushToken token2 = NotificationPushToken.create(user, "token-2", PushPlatform.ANDROID);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(token1, token2));
        willThrow(new RuntimeException("push fail"))
                .given(notificationPushSender)
                .send(eq("token-1"), eq(PushPlatform.WEB), any(String.class), any(String.class));

        notificationPushDispatchService.send(notification);

        verify(notificationPushSender, times(2))
                .send(any(String.class), any(PushPlatform.class), any(String.class), any(String.class));
    }

    @Test
    void tokenHashDoesNotExposeRawToken() {
        String hash = notificationPushDispatchService.tokenHash("raw-token-value-123");

        assertThat(hash).isNotBlank();
        assertThat(hash).doesNotContain("raw-token-value-123");
    }

    private User activeUser(Long id, String email, String nickname) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .nickname(nickname)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
