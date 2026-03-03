package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.dto.response.NotificationPushTokenResponse;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPushTokenServiceTest {

    @Mock
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPushTokenService notificationPushTokenService;

    @Test
    void registerPushTokenCreatesTokenWhenNotExists() {
        User user = activeUser(1L, "member@example.com", "member");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationPushTokenRepository.findByToken("token-1")).willReturn(Optional.empty());
        given(notificationPushTokenRepository.save(any(NotificationPushToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NotificationPushTokenResponse response =
                notificationPushTokenService.registerPushToken(1L, "token-1", PushPlatform.WEB);

        ArgumentCaptor<NotificationPushToken> captor = ArgumentCaptor.forClass(NotificationPushToken.class);
        verify(notificationPushTokenRepository).save(captor.capture());
        NotificationPushToken saved = captor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
        assertThat(saved.getToken()).isEqualTo("token-1");
        assertThat(saved.getPlatform()).isEqualTo(PushPlatform.WEB);
        assertThat(saved.isActive()).isTrue();

        assertThat(response.token()).isEqualTo("token-1");
        assertThat(response.platform()).isEqualTo(PushPlatform.WEB);
        assertThat(response.active()).isTrue();
    }

    @Test
    void registerPushTokenReactivatesExistingToken() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User nextUser = activeUser(2L, "next@example.com", "next");
        NotificationPushToken existing = NotificationPushToken.create(
                owner,
                "token-1",
                PushPlatform.WEB
        );
        existing.deactivate();

        given(userRepository.findById(2L)).willReturn(Optional.of(nextUser));
        given(notificationPushTokenRepository.findByToken("token-1")).willReturn(Optional.of(existing));

        NotificationPushTokenResponse response =
                notificationPushTokenService.registerPushToken(2L, "token-1", PushPlatform.ANDROID);

        verify(notificationPushTokenRepository, never()).save(any(NotificationPushToken.class));
        assertThat(existing.getUser().getId()).isEqualTo(2L);
        assertThat(existing.getPlatform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(existing.isActive()).isTrue();
        assertThat(response.platform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(response.active()).isTrue();
    }

    @Test
    void registerPushTokenRecoversFromUniqueKeyRace() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User nextUser = activeUser(2L, "next@example.com", "next");
        NotificationPushToken existing = NotificationPushToken.create(
                owner,
                "token-race",
                PushPlatform.WEB
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(nextUser));
        given(notificationPushTokenRepository.findByToken("token-race"))
                .willReturn(Optional.empty(), Optional.of(existing));
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(notificationPushTokenRepository)
                .save(any(NotificationPushToken.class));

        NotificationPushTokenResponse response =
                notificationPushTokenService.registerPushToken(2L, "token-race", PushPlatform.ANDROID);

        assertThat(existing.getUser().getId()).isEqualTo(2L);
        assertThat(existing.getPlatform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(existing.isActive()).isTrue();
        assertThat(response.token()).isEqualTo("token-race");
        assertThat(response.platform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(response.active()).isTrue();
    }

    @Test
    void registerPushTokenThrowsConflictWhenUniqueRaceCannotBeRecovered() {
        User user = activeUser(1L, "member@example.com", "member");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationPushTokenRepository.findByToken("token-race"))
                .willReturn(Optional.empty(), Optional.empty());
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(notificationPushTokenRepository)
                .save(any(NotificationPushToken.class));

        assertThatThrownBy(() -> notificationPushTokenService.registerPushToken(1L, "token-race", PushPlatform.WEB))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void unregisterPushTokenDeactivatesOwnedToken() {
        User user = activeUser(1L, "member@example.com", "member");
        NotificationPushToken token = NotificationPushToken.create(user, "token-1", PushPlatform.WEB);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationPushTokenRepository.findByUserIdAndToken(1L, "token-1"))
                .willReturn(Optional.of(token));

        boolean removed = notificationPushTokenService.unregisterPushToken(1L, "token-1");

        assertThat(removed).isTrue();
        assertThat(token.isActive()).isFalse();
        verify(notificationPushTokenRepository).save(token);
    }

    @Test
    void unregisterPushTokenReturnsFalseWhenMissing() {
        User user = activeUser(1L, "member@example.com", "member");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationPushTokenRepository.findByUserIdAndToken(1L, "token-1"))
                .willReturn(Optional.empty());

        boolean removed = notificationPushTokenService.unregisterPushToken(1L, "token-1");

        assertThat(removed).isFalse();
        verify(notificationPushTokenRepository, never()).save(any(NotificationPushToken.class));
    }

    @Test
    void registerPushTokenThrowsWhenUserMissing() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationPushTokenService.registerPushToken(999L, "token-1", PushPlatform.WEB))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
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
