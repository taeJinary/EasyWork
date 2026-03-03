package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushRetryJob;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
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
    private NotificationPushDispatchService notificationPushDispatchService;

    @InjectMocks
    private NotificationPushRetryService notificationPushRetryService;

    @Test
    void enqueueFailureSavesPendingJobWhenNoOpenJobExists() {
        Notification notification = notification(101L);
        given(notificationPushRetryJobRepository.existsByNotificationIdAndCompletedAtIsNull(101L))
                .willReturn(false);

        notificationPushRetryService.enqueueFailure(notification, "temporary push failure");

        ArgumentCaptor<NotificationPushRetryJob> captor = ArgumentCaptor.forClass(NotificationPushRetryJob.class);
        verify(notificationPushRetryJobRepository).save(captor.capture());
        NotificationPushRetryJob saved = captor.getValue();
        assertThat(saved.getNotificationId()).isEqualTo(101L);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getNextRetryAt()).isNotNull();
        assertThat(saved.getLastErrorMessage()).contains("temporary push failure");
    }

    @Test
    void enqueueFailureSkipsWhenOpenJobAlreadyExists() {
        Notification notification = notification(101L);
        given(notificationPushRetryJobRepository.existsByNotificationIdAndCompletedAtIsNull(101L))
                .willReturn(true);

        notificationPushRetryService.enqueueFailure(notification, "temporary push failure");

        verify(notificationPushRetryJobRepository, never()).save(any(NotificationPushRetryJob.class));
    }

    @Test
    void retryPendingPushesMarksJobCompletedWhenDispatchSucceeds() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        Notification notification = notification(101L);

        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushDispatchService.send(notification)).willReturn(false);

        notificationPushRetryService.retryPendingPushes(50);

        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getRetryCount()).isEqualTo(0);
        verify(notificationPushRetryJobRepository).save(job);
    }

    @Test
    void retryPendingPushesReschedulesWhenDispatchStillFails() {
        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                101L,
                LocalDateTime.now().minusMinutes(1),
                "temporary push failure"
        );
        Notification notification = notification(101L);

        given(notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(notificationRepository.findById(101L)).willReturn(Optional.of(notification));
        given(notificationPushDispatchService.send(notification)).willReturn(true);

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
        verify(notificationPushDispatchService, never()).send(any(Notification.class));
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
        org.springframework.test.util.ReflectionTestUtils.setField(notification, "id", notificationId);
        return notification;
    }
}
