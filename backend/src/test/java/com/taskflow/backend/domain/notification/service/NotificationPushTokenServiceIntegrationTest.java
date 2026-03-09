package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.dto.response.NotificationPushTokenResponse;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPushTokenServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPushTokenService notificationPushTokenService;

    @Autowired
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Test
    void registerExistingTokenReassignsAndReactivatesForLatestUser() {
        User firstUser = saveActiveUser("push-first-user");
        User secondUser = saveActiveUser("push-second-user");

        notificationPushTokenService.registerPushToken(firstUser.getId(), "token-1", PushPlatform.WEB);
        boolean removed = notificationPushTokenService.unregisterPushToken(firstUser.getId(), "token-1");
        NotificationPushTokenResponse response =
                notificationPushTokenService.registerPushToken(secondUser.getId(), "token-1", PushPlatform.ANDROID);

        NotificationPushToken saved = notificationPushTokenRepository.findByToken("token-1").orElseThrow();
        List<NotificationPushToken> firstUserActiveTokens =
                notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(firstUser.getId());
        List<NotificationPushToken> secondUserActiveTokens =
                notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(secondUser.getId());

        assertThat(removed).isTrue();
        assertThat(response.token()).isEqualTo("token-1");
        assertThat(response.platform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(response.active()).isTrue();
        assertThat(saved.getUser().getId()).isEqualTo(secondUser.getId());
        assertThat(saved.getPlatform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(saved.isActive()).isTrue();
        assertThat(firstUserActiveTokens).isEmpty();
        assertThat(secondUserActiveTokens).hasSize(1);
    }

    @Test
    void unregisterUnknownTokenReturnsFalse() {
        User user = saveActiveUser("push-missing-user");

        boolean removed = notificationPushTokenService.unregisterPushToken(user.getId(), "missing-token");

        assertThat(removed).isFalse();
    }

    @Test
    void getActivePushTokensReturnsOnlyCurrentUsersActiveTokens() {
        User user = saveActiveUser("push-list-user");
        User otherUser = saveActiveUser("push-list-other");

        notificationPushTokenService.registerPushToken(user.getId(), "token-1", PushPlatform.WEB);
        notificationPushTokenService.registerPushToken(user.getId(), "token-2", PushPlatform.ANDROID);
        notificationPushTokenService.registerPushToken(otherUser.getId(), "token-3", PushPlatform.IOS);
        notificationPushTokenService.unregisterPushToken(user.getId(), "token-1");

        List<NotificationPushTokenResponse> response = notificationPushTokenService.getActivePushTokens(user.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().token()).isEqualTo("token-2");
        assertThat(response.getFirst().platform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(response.getFirst().active()).isTrue();
    }

    private User saveActiveUser(String nicknamePrefix) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname(normalizeNickname(nicknamePrefix))
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String normalizeNickname(String nicknamePrefix) {
        return nicknamePrefix.length() <= 20
                ? nicknamePrefix
                : nicknamePrefix.substring(0, 20);
    }
}
