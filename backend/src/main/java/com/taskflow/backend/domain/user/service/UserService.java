package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.dto.request.ChangePasswordRequest;
import com.taskflow.backend.domain.user.dto.request.UpdateProfileRequest;
import com.taskflow.backend.domain.user.dto.request.WithdrawRequest;
import com.taskflow.backend.domain.user.dto.response.UserProfileResponse;
import com.taskflow.backend.domain.user.entity.PasswordHistory;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

    public UserProfileResponse getMyProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        user.updateProfile(request.nickname(), null);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        List<PasswordHistory> recentPasswordHistories =
                passwordHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId);
        boolean reused = recentPasswordHistories.stream()
                .anyMatch(history -> passwordEncoder.matches(request.newPassword(), history.getPasswordHash()));
        if (reused) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);
        passwordHistoryRepository.save(PasswordHistory.create(user, encodedNewPassword));
        redisService.delete(refreshTokenKey(userId));
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.softDelete();
        redisService.delete(refreshTokenKey(userId));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}

