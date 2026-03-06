package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.dto.request.ChangePasswordRequest;
import com.taskflow.backend.domain.user.dto.request.WithdrawRequest;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.infra.redis.RedisService;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisService redisService;

    @Test
    void changePasswordStoresHistoryAndDeletesRefreshTokens() {
        User user = saveActiveUser("user-change-password", "OldPass123!");
        redisService.setValue("refresh:" + user.getId() + ":device-1", "token-1", Duration.ofMinutes(30));
        redisService.setValue("refresh:" + user.getId() + ":device-2", "token-2", Duration.ofMinutes(30));

        userService.changePassword(
                user.getId(),
                new ChangePasswordRequest("OldPass123!", "NewPass123!")
        );

        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        assertThat(passwordEncoder.matches("NewPass123!", reloaded.getPassword())).isTrue();
        assertThat(passwordHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(1);
        assertThat(redisService.hasKey("refresh:" + user.getId() + ":device-1")).isFalse();
        assertThat(redisService.hasKey("refresh:" + user.getId() + ":device-2")).isFalse();
    }

    @Test
    void withdrawSoftDeletesUserAndDeletesRefreshTokens() {
        User user = saveActiveUser("user-withdraw", "Withdraw123!");
        redisService.setValue("refresh:" + user.getId() + ":device-1", "token-1", Duration.ofMinutes(30));

        userService.withdraw(user.getId(), new WithdrawRequest("Withdraw123!"));

        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        assertThat(reloaded.isDeleted()).isTrue();
        assertThat(redisService.hasKey("refresh:" + user.getId() + ":device-1")).isFalse();
    }

    private User saveActiveUser(String nicknamePrefix, String rawPassword) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nicknamePrefix)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
