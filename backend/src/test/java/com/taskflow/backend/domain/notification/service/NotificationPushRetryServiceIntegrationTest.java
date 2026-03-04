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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
}
