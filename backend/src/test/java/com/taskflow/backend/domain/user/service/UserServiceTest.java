package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.dto.request.ChangePasswordRequest;
import com.taskflow.backend.domain.user.dto.request.UpdateProfileRequest;
import com.taskflow.backend.domain.user.dto.request.WithdrawRequest;
import com.taskflow.backend.domain.user.dto.response.UserProfileResponse;
import com.taskflow.backend.domain.user.entity.PasswordHistory;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private UserService userService;

    @Test
    void getMyProfileReturnsProfileWhenUserExists() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .nickname("tester")
                .profileImg("https://cdn.example.com/profile.png")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.getMyProfile(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.profileImg()).isEqualTo("https://cdn.example.com/profile.png");
        assertThat(response.provider()).isEqualTo("LOCAL");
    }

    @Test
    void getMyProfileThrowsWhenUserIsNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getMyProfileThrowsWhenUserIsDeleted() {
        User deletedUser = User.builder()
                .id(1L)
                .email("deleted@example.com")
                .password("encoded")
                .nickname("deleted")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.DELETED)
                .deletedAt(LocalDateTime.now())
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> userService.getMyProfile(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateMyProfileUpdatesNickname() {
        User user = activeUser();
        UpdateProfileRequest request = new UpdateProfileRequest("길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.updateMyProfile(1L, request);

        assertThat(response.nickname()).isEqualTo("길동");
    }

    @Test
    void changePasswordChangesPasswordWhenCurrentPasswordMatches() {
        User user = activeUser();
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123!", "NewPass123!");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("OldPass123!", user.getPassword())).willReturn(true);
        given(passwordHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());
        given(passwordEncoder.encode("NewPass123!")).willReturn("encoded-new");

        userService.changePassword(1L, request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(passwordHistoryRepository).save(any(PasswordHistory.class));
        verify(redisService).deleteByPattern("refresh:1:*");
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordMismatches() {
        User user = activeUser();
        ChangePasswordRequest request = new ChangePasswordRequest("Wrong123!", "NewPass123!");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Wrong123!", user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

        verify(passwordHistoryRepository, never()).save(any(PasswordHistory.class));
        verify(redisService, never()).deleteByPattern("refresh:1:*");
    }

    @Test
    void changePasswordThrowsWhenNewPasswordWasRecentlyUsed() {
        User user = activeUser();
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123!", "ReusedPass123!");
        PasswordHistory history = PasswordHistory.create(user, "encoded-old");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("OldPass123!", user.getPassword())).willReturn(true);
        given(passwordHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(history));
        given(passwordEncoder.matches("ReusedPass123!", "encoded-old")).willReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(passwordHistoryRepository, never()).save(any(PasswordHistory.class));
    }

    @Test
    void withdrawSoftDeletesUserWhenPasswordMatches() {
        User user = activeUser();
        WithdrawRequest request = new WithdrawRequest("OldPass123!");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("OldPass123!", user.getPassword())).willReturn(true);

        userService.withdraw(1L, request);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        verify(redisService).deleteByPattern("refresh:1:*");
    }

    @Test
    void withdrawThrowsWhenPasswordMismatches() {
        User user = activeUser();
        WithdrawRequest request = new WithdrawRequest("Wrong123!");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Wrong123!", user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.withdraw(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

        verify(redisService, never()).deleteByPattern("refresh:1:*");
    }

    private User activeUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-old")
                .nickname("tester")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
