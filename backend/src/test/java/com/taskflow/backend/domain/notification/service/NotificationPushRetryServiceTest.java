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
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @InjectMocks
    private NotificationPushRetryService notificationPushRetryService;

    @BeforeEach
    void setUpDefaultRetryPolicy() {
        ReflectionTestUtils.setField(notificationPushRetryService, "retryDelaySeconds", 300L);
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryAttempts", 10);
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryDelaySeconds", 3600L);
    }

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
        assertThat(saved.getOpenKey()).isEqualTo("101:501");
        assertThat(saved.getNextRetryAt()).isNotNull();
        assertThat(saved.getLastErrorMessage()).contains("temporary push failure");
        verify(operationalMetricsService).incrementNotificationPushRetryEnqueued();
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
    void enqueueFailureIgnoresDuplicateOpenJobRaceCondition() {
        Notification notification = notification(101L);
        given(notificationPushRetryJobRepository.existsByNotificationIdAndPushTokenIdAndCompletedAtIsNull(101L, 501L))
                .willReturn(false);
        given(notificationPushRetryJobRepository.save(any(NotificationPushRetryJob.class)))
                .willThrow(new DataIntegrityViolationException(
                        "Duplicate entry '101:501' for key 'uk_notification_push_retry_jobs_open_key'"
                ));

        assertThatCode(() -> notificationPushRetryService.enqueueFailure(notification, 501L, "temporary push failure"))
                .doesNotThrowAnyException();

        verify(notificationPushRetryJobRepository).save(any(NotificationPushRetryJob.class));
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
        verify(operationalMetricsService).incrementNotificationPushRetryCompleted();
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
        verify(operationalMetricsService).incrementNotificationPushRetryRescheduled();
    }

    @Test
    void retryPendingPushesDoesNotDoubleMarkOrRecordMetricWhenSaveFailsAfterTransientFailure() {
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
        given(notificationPushRetryJobRepository.save(job)).willThrow(new RuntimeException("db write failed"));

        assertThatThrownBy(() -> notificationPushRetryService.retryPendingPushes(50))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db write failed");

        assertThat(job.getRetryCount()).isEqualTo(1);
        verify(notificationPushRetryJobRepository).save(job);
        verify(operationalMetricsService).incrementNotificationPushRetryPersistenceFailure();
        verify(operationalMetricsService, never()).incrementNotificationPushRetryRescheduled();
        verify(operationalMetricsService, never()).incrementNotificationPushRetryDeadLetter();
        verify(operationalMetricsService, never()).incrementNotificationPushRetryCompleted();
    }

    @Test
    void retryPendingPushesDoesNotRecordCompletedMetricWhenSaveFailsAfterCompletion() {
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
        given(notificationPushRetryJobRepository.save(job)).willThrow(new RuntimeException("db write failed"));

        assertThatThrownBy(() -> notificationPushRetryService.retryPendingPushes(50))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db write failed");

        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getRetryCount()).isEqualTo(0);
        verify(notificationPushRetryJobRepository).save(job);
        verify(operationalMetricsService).incrementNotificationPushRetryPersistenceFailure();
        verify(operationalMetricsService, never()).incrementNotificationPushRetryCompleted();
        verify(operationalMetricsService, never()).incrementNotificationPushRetryRescheduled();
        verify(operationalMetricsService, never()).incrementNotificationPushRetryDeadLetter();
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

    @Test
    void retryPendingPushesMarksCompletedWhenMaxRetryAttemptsReached() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                501L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        job.markFailed("first failure", LocalDateTime.now().minusSeconds(20));
        job.markFailed("second failure", LocalDateTime.now().minusSeconds(10));

        Notification notification = notification(101L);
        NotificationPushToken pushToken = activePushToken(501L, notification.getUser());
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryAttempts", 3);
        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushTokenRepository.findById(501L)).willReturn(Optional.of(pushToken));
        given(notificationPushDispatchService.sendToToken(notification, pushToken)).willReturn(true);

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getRetryCount()).isEqualTo(3);
        assertThat(job.getLastErrorMessage()).contains("Transient push delivery failure");
        verify(notificationPushRetryJobRepository).save(job);
        verify(operationalMetricsService).incrementNotificationPushRetryDeadLetter();
    }

    @Test
    void retryPendingPushesUsesExponentialBackoffDelay() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                501L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        job.markFailed("first failure", LocalDateTime.now().minusSeconds(20));
        job.markFailed("second failure", LocalDateTime.now().minusSeconds(10));

        Notification notification = notification(101L);
        NotificationPushToken pushToken = activePushToken(501L, notification.getUser());
        ReflectionTestUtils.setField(notificationPushRetryService, "retryDelaySeconds", 30L);
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryAttempts", 10);
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryDelaySeconds", 600L);
        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushTokenRepository.findById(501L)).willReturn(Optional.of(pushToken));
        given(notificationPushDispatchService.sendToToken(notification, pushToken)).willReturn(true);
        LocalDateTime startedAt = LocalDateTime.now();

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(3);
        assertThat(job.getNextRetryAt()).isAfter(startedAt.plusSeconds(110));
        assertThat(job.getNextRetryAt()).isBefore(startedAt.plusSeconds(130));
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
