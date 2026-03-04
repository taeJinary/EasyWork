package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPushRetryServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private NotificationPushRetryService notificationPushRetryService;

    @Autowired
    private NotificationPushRetryJobRepository notificationPushRetryJobRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private NotificationPushDispatchService notificationPushDispatchService;

    @Test
    void enqueueFailureKeepsSingleOpenJobPerNotificationAndToken() {
        User user = userRepository.save(User.builder()
                .email("push-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname("push-user")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());

        Notification notification = notificationRepository.save(Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "title",
                "content",
                NotificationReferenceType.PROJECT,
                10L
        ));

        NotificationPushToken token = notificationPushTokenRepository.save(
                NotificationPushToken.create(user, "token-" + System.nanoTime(), PushPlatform.WEB)
        );

        notificationPushRetryService.enqueueFailure(notification, token.getId(), "first error");
        notificationPushRetryService.enqueueFailure(notification, token.getId(), "second error");

        long openJobs = notificationPushRetryJobRepository.findAll().stream()
                .filter(job -> job.getCompletedAt() == null)
                .count();

        assertThat(openJobs).isEqualTo(1L);
    }

    @Test
    void retryPendingPushesCompletesOpenJobWhenDispatchSucceeds() {
        User user = userRepository.save(User.builder()
                .email("push-retry-success-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname("push-retry-success")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());

        Notification notification = notificationRepository.save(Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "title",
                "content",
                NotificationReferenceType.PROJECT,
                20L
        ));

        NotificationPushToken token = notificationPushTokenRepository.save(
                NotificationPushToken.create(user, "retry-success-token-" + System.nanoTime(), PushPlatform.WEB)
        );

        notificationPushRetryService.enqueueFailure(notification, token.getId(), "first transient failure");
        given(notificationPushDispatchService.sendToToken(any(Notification.class), any(NotificationPushToken.class)))
                .willReturn(false);

        notificationPushRetryService.retryPendingPushes(50);

        var jobs = notificationPushRetryJobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getCompletedAt()).isNotNull();
        assertThat(jobs.get(0).getOpenKey()).isNull();
        assertThat(jobs.get(0).getLastErrorMessage()).isNull();
        assertThat(jobs.get(0).getRetryCount()).isEqualTo(0);
    }

    @Test
    void retryPendingPushesMarksDeadLetterWhenTransientFailureRepeatsToMaxAttempts() {
        User user = userRepository.save(User.builder()
                .email("push-retry-dead-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname("push-retry-dead")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());

        Notification notification = notificationRepository.save(Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "title",
                "content",
                NotificationReferenceType.PROJECT,
                30L
        ));

        NotificationPushToken token = notificationPushTokenRepository.save(
                NotificationPushToken.create(user, "retry-dead-token-" + System.nanoTime(), PushPlatform.WEB)
        );

        notificationPushRetryService.enqueueFailure(notification, token.getId(), "first transient failure");
        ReflectionTestUtils.setField(notificationPushRetryService, "retryDelaySeconds", 1L);
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryAttempts", 2);
        ReflectionTestUtils.setField(notificationPushRetryService, "maxRetryDelaySeconds", 1L);

        given(notificationPushDispatchService.sendToToken(any(Notification.class), any(NotificationPushToken.class)))
                .willReturn(true);

        notificationPushRetryService.retryPendingPushes(50);

        var firstCycleJob = notificationPushRetryJobRepository.findAll().get(0);
        assertThat(firstCycleJob.getCompletedAt()).isNull();
        assertThat(firstCycleJob.getRetryCount()).isEqualTo(1);
        assertThat(firstCycleJob.getOpenKey()).isNotNull();

        ReflectionTestUtils.setField(firstCycleJob, "nextRetryAt", LocalDateTime.now().minusSeconds(1));
        notificationPushRetryJobRepository.saveAndFlush(firstCycleJob);

        notificationPushRetryService.retryPendingPushes(50);

        var secondCycleJob = notificationPushRetryJobRepository.findAll().get(0);
        assertThat(secondCycleJob.getCompletedAt()).isNotNull();
        assertThat(secondCycleJob.getRetryCount()).isEqualTo(2);
        assertThat(secondCycleJob.getOpenKey()).isNull();
        assertThat(secondCycleJob.getLastErrorMessage()).contains("Transient push delivery failure");
    }

    @Test
    void retryPendingPushesCompletesJobWithoutDispatchWhenTokenIsInactive() {
        User user = userRepository.save(User.builder()
                .email("push-retry-inactive-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname("push-retry-inactive")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());

        Notification notification = notificationRepository.save(Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "title",
                "content",
                NotificationReferenceType.PROJECT,
                40L
        ));

        NotificationPushToken token = notificationPushTokenRepository.save(
                NotificationPushToken.create(user, "retry-inactive-token-" + System.nanoTime(), PushPlatform.WEB)
        );
        token.deactivate();
        notificationPushTokenRepository.saveAndFlush(token);

        notificationPushRetryService.enqueueFailure(notification, token.getId(), "first transient failure");
        Mockito.clearInvocations(notificationPushDispatchService);

        notificationPushRetryService.retryPendingPushes(50);

        var jobs = notificationPushRetryJobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getCompletedAt()).isNotNull();
        verify(notificationPushDispatchService, never()).sendToToken(any(Notification.class), any(NotificationPushToken.class));
    }
}
