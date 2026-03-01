package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.AuthUserResponse;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.user.service.model.LoginTokens;
import com.taskflow.backend.domain.user.service.model.ReissueTokens;
import com.taskflow.backend.global.auth.jwt.JwtProperties;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String LOGIN_FAIL_KEY_PREFIX = "auth:login-fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "auth:login-lock:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisService redisService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        return SignupResponse.from(savedUser);
    }

    @Transactional
    public LoginTokens login(LoginRequest request) {
        String email = request.email();
        ensureNotLocked(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> invalidCredentialsWithFailure(email));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentialsWithFailure(email);
        }

        clearLoginFailure(email);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        redisService.setValue(
                refreshTokenKey(user.getId()),
                refreshToken,
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
        );

        return new LoginTokens(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration(),
                AuthUserResponse.from(user)
        );
    }

    @Transactional
    public ReissueTokens reissue(String refreshToken) {
        Long userId = extractUserIdFromToken(refreshToken);
        String tokenKey = refreshTokenKey(userId);

        String storedToken = redisService.getValue(tokenKey).orElse(null);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        redisService.setValue(
                tokenKey,
                newRefreshToken,
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
        );

        return new ReissueTokens(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessTokenExpiration()
        );
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            long remainingExpiration = jwtTokenProvider.getRemainingExpiration(accessToken);
            if (remainingExpiration > 0) {
                redisService.setValue(
                        blackListKey(accessToken),
                        "logout",
                        Duration.ofMillis(remainingExpiration)
                );
            }
        }

        Long userId = extractUserIdFromToken(refreshToken);
        redisService.delete(refreshTokenKey(userId));
    }

    private void ensureNotLocked(String email) {
        if (redisService.hasKey(loginLockKey(email))) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    private BusinessException invalidCredentialsWithFailure(String email) {
        handleLoginFailure(email);
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    private void handleLoginFailure(String email) {
        String failKey = loginFailKey(email);
        Long failCount = redisService.increment(failKey);
        redisService.expire(failKey, LOGIN_LOCK_DURATION);

        if (failCount != null && failCount >= MAX_LOGIN_ATTEMPTS) {
            redisService.setValue(loginLockKey(email), "LOCKED", LOGIN_LOCK_DURATION);
        }
    }

    private void clearLoginFailure(String email) {
        redisService.delete(loginFailKey(email));
        redisService.delete(loginLockKey(email));
    }

    private Long extractUserIdFromToken(String token) {
        try {
            return jwtTokenProvider.getUserId(token);
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    private String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }

    private String loginFailKey(String email) {
        return LOGIN_FAIL_KEY_PREFIX + email;
    }

    private String loginLockKey(String email) {
        return LOGIN_LOCK_KEY_PREFIX + email;
    }

    private String blackListKey(String accessToken) {
        return BLACKLIST_KEY_PREFIX + accessToken;
    }
}

