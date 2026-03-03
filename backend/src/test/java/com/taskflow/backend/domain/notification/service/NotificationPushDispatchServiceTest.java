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
import org.springframework.test.util.ReflectionTestUtils;

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
        ReflectionTestUtils.setField(web, "id", 11L);
        ReflectionTestUtils.setField(android, "id", 12L);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(web, android));

        NotificationPushDispatchService.NotificationPushDispatchResult result =
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
        assertThat(result.transientFailedTokenIds()).isEmpty();
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
        ReflectionTestUtils.setField(token1, "id", 21L);
        ReflectionTestUtils.setField(token2, "id", 22L);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(token1, token2));
        willThrow(new PushDeliveryRetryableException("push transient fail"))
                .given(notificationPushSender)
                .send(eq("token-1"), eq(PushPlatform.WEB), any(String.class), any(String.class));

        NotificationPushDispatchService.NotificationPushDispatchResult result =
                notificationPushDispatchService.send(notification);

        verify(notificationPushSender, times(2))
                .send(any(String.class), any(PushPlatform.class), any(String.class), any(String.class));
        assertThat(result.transientFailedTokenIds()).containsExactly(21L);
    }

    @Test
    void sendDoesNotMarkTransientFailureWhenNonRetryableErrorOccurs() {
        User user = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );

        NotificationPushToken token = NotificationPushToken.create(user, "token-1", PushPlatform.WEB);
        ReflectionTestUtils.setField(token, "id", 41L);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(token));
        willThrow(new PushDeliveryNonRetryableException("fcm server key missing"))
                .given(notificationPushSender)
                .send(eq("token-1"), eq(PushPlatform.WEB), any(String.class), any(String.class));

        NotificationPushDispatchService.NotificationPushDispatchResult result =
                notificationPushDispatchService.send(notification);

        assertThat(result.transientFailedTokenIds()).isEmpty();
    }

    @Test
    void tokenHashDoesNotExposeRawToken() {
        String hash = notificationPushDispatchService.tokenHash("raw-token-value-123");

        assertThat(hash).isNotBlank();
        assertThat(hash).doesNotContain("raw-token-value-123");
    }

    @Test
    void sendDeactivatesTokenWhenDeliveryFailsWithInvalidTokenError() {
        User user = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );

        NotificationPushToken token = NotificationPushToken.create(user, "expired-token", PushPlatform.WEB);
        ReflectionTestUtils.setField(token, "id", 31L);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(token));
        willThrow(new PushTokenInvalidException("NotRegistered"))
                .given(notificationPushSender)
                .send(eq("expired-token"), eq(PushPlatform.WEB), any(String.class), any(String.class));

        NotificationPushDispatchService.NotificationPushDispatchResult result =
                notificationPushDispatchService.send(notification);

        assertThat(token.isActive()).isFalse();
        verify(notificationPushTokenRepository).save(token);
        assertThat(result.transientFailedTokenIds()).isEmpty();
    }

    @Test
    void sendDoesNotEnqueueRetryWhenUnexpectedRuntimeOccurs() {
        User user = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );

        NotificationPushToken token = NotificationPushToken.create(user, "token-1", PushPlatform.WEB);
        ReflectionTestUtils.setField(token, "id", 51L);
        given(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(1L))
                .willReturn(List.of(token));
        willThrow(new IllegalStateException("fcm misconfigured"))
                .given(notificationPushSender)
                .send(eq("token-1"), eq(PushPlatform.WEB), any(String.class), any(String.class));

        NotificationPushDispatchService.NotificationPushDispatchResult result =
                notificationPushDispatchService.send(notification);

        assertThat(result.transientFailedTokenIds()).isEmpty();
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
