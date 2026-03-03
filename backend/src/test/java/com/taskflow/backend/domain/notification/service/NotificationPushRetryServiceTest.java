package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushRetryJob;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPushRetryServiceTest {

    @Mock
    private NotificationPushRetryJobRepository notificationPushRetryJobRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Mock
    private NotificationPushDispatchService notificationPushDispatchService;

    @InjectMocks
    private NotificationPushRetryService notificationPushRetryService;

    @Test
    void enqueueFailureSavesPendingJobWhenNoOpenJobExists() {
        Notification notification = notification(101L);
        given(notificationPushRetryJobRepository.existsByNotificationIdAndPushTokenIdAndCompletedAtIsNull(101L, 501L))
                .willReturn(false);

        notificationPushRetryService.enqueueFailure(notification, 501L, "temporary push failure");

        ArgumentCaptor<NotificationPushRetryJob> captor = ArgumentCaptor.forClass(NotificationPushRetryJob.class);
        verify(notificationPushRetryJobRepository).save(captor.capture());
        NotificationPushRetryJob saved = captor.getValue();
        assertThat(saved.getNotificationId()).isEqualTo(101L);
        assertThat(saved.getPushTokenId()).isEqualTo(501L);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getNextRetryAt()).isNotNull();
        assertThat(saved.getLastErrorMessage()).contains("temporary push failure");
    }

    @Test
    void enqueueFailureSkipsWhenOpenJobAlreadyExists() {
        Notification notification = notification(101L);
        given(notificationPushRetryJobRepository.existsByNotificationIdAndPushTokenIdAndCompletedAtIsNull(101L, 501L))
                .willReturn(true);

        notificationPushRetryService.enqueueFailure(notification, 501L, "temporary push failure");

        verify(notificationPushRetryJobRepository, never()).save(any(NotificationPushRetryJob.class));
    }

    @Test
    void retryPendingPushesMarksJobCompletedWhenDispatchToFailedTokenSucceeds() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                501L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        Notification notification = notification(101L);
        NotificationPushToken pushToken = activePushToken(501L, notification.getUser());

        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushTokenRepository.findById(501L)).willReturn(Optional.of(pushToken));
        given(notificationPushDispatchService.sendToToken(notification, pushToken)).willReturn(false);

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getRetryCount()).isEqualTo(0);
        verify(notificationPushRetryJobRepository).save(job);
    }

    @Test
    void retryPendingPushesReschedulesWhenDispatchToFailedTokenStillFails() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                501L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        Notification notification = notification(101L);
        NotificationPushToken pushToken = activePushToken(501L, notification.getUser());

        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushTokenRepository.findById(501L)).willReturn(Optional.of(pushToken));
        given(notificationPushDispatchService.sendToToken(notification, pushToken)).willReturn(true);

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getLastErrorMessage()).contains("Transient push delivery failure");
        assertThat(job.getNextRetryAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        verify(notificationPushRetryJobRepository).save(job);
    }

    @Test
    void retryPendingPushesMarksCompletedWhenNotificationIsMissing() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                501L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );

        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.empty());

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNotNull();
        verify(notificationPushTokenRepository, never()).findById(any(Long.class));
        verify(notificationPushDispatchService, never()).sendToToken(any(Notification.class), any(NotificationPushToken.class));
        verify(notificationPushRetryJobRepository).save(job);
    }

    @Test
    void retryPendingPushesMarksCompletedWhenPushTokenIsMissing() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                501L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        Notification notification = notification(101L);

        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushTokenRepository.findById(501L)).willReturn(Optional.empty());

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNotNull();
        verify(notificationPushDispatchService, never()).sendToToken(any(Notification.class), any(NotificationPushToken.class));
        verify(notificationPushRetryJobRepository).save(job);
    }

    private Notification notification(Long notificationId) {
        User user = User.builder()
                .id(1L)
                .email("member@example.com")
                .password("encoded")
                .nickname("member")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        Notification notification = Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );
        ReflectionTestUtils.setField(notification, "id", notificationId);
        return notification;
    }

    private NotificationPushToken activePushToken(Long tokenId, User user) {
        NotificationPushToken pushToken = NotificationPushToken.create(user, "token-" + tokenId, PushPlatform.WEB);
        ReflectionTestUtils.setField(pushToken, "id", tokenId);
        return pushToken;
    }
}
