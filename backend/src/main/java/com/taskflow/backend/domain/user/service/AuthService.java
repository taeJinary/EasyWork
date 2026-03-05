package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthCodeLoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthLoginRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.AuthUserResponse;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.entity.PasswordHistory;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.user.service.oauth.OAuthClientRegistry;
import com.taskflow.backend.domain.user.service.oauth.OAuthAccessTokenExchanger;
import com.taskflow.backend.domain.user.service.oauth.OAuthProfile;
import com.taskflow.backend.domain.user.service.model.LoginTokens;
import com.taskflow.backend.domain.user.service.model.ReissueTokens;
import com.taskflow.backend.global.auth.jwt.JwtProperties;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import com.taskflow.backend.infra.redis.RedisService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "login:lock:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisService redisService;
    private final OAuthClientRegistry oauthClientRegistry;
    private final OAuthAccessTokenExchanger oauthAccessTokenExchanger;
    private final OperationalMetricsService operationalMetricsService;

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
        passwordHistoryRepository.save(PasswordHistory.create(savedUser, savedUser.getPassword()));
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
        return issueLoginTokens(user);
    }

    @Transactional
    public LoginTokens oauthLogin(OAuthLoginRequest request) {
        OAuthProvider provider = request.provider();
        OAuthProfile profile = oauthClientRegistry.getClient(provider).fetchProfile(request.accessToken());
        validateOAuthProfile(profile);

        User user = userRepository.findByProviderAndProviderId(provider.name(), profile.providerId())
                .orElseGet(() -> createOAuthUser(provider, profile));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        return issueLoginTokens(user);
    }

    @Transactional
    public LoginTokens oauthCodeLogin(OAuthCodeLoginRequest request) {
        String accessToken = oauthAccessTokenExchanger.exchange(
                request.provider(),
                request.authorizationCode(),
                request.codeVerifier(),
                request.state()
        );
        return oauthLogin(new OAuthLoginRequest(request.provider(), accessToken));
    }

    @Transactional
    public ReissueTokens reissue(String refreshToken) {
        try {
            Long userId = extractUserIdFromToken(refreshToken);
            String sessionId = extractSessionIdFromRefreshToken(refreshToken);
            String tokenKey = refreshTokenKey(userId, sessionId);

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
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), sessionId);

            redisService.setValue(
                    tokenKey,
                    newRefreshToken,
                    Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
            );

            return new ReissueTokens(
                    newAccessToken,
                    newRefreshToken,
                    jwtProperties.getAccessTokenExpiration(),
                    jwtProperties.getRefreshTokenExpiration()
            );
        } catch (BusinessException exception) {
            operationalMetricsService.incrementRefreshReissueFailure();
            throw exception;
        }
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            long remainingExpiration = jwtTokenProvider.getRemainingExpiration(accessToken);
            String accessTokenId = jwtTokenProvider.getTokenId(accessToken);
            if (remainingExpiration > 0 && accessTokenId != null && !accessTokenId.isBlank()) {
                redisService.setValue(
                        blackListKey(accessTokenId),
                        "logout",
                        Duration.ofMillis(remainingExpiration)
                );
            }
        }

        Long userId = extractUserIdFromToken(refreshToken);
        String sessionId = extractSessionIdFromRefreshToken(refreshToken);
        redisService.delete(refreshTokenKey(userId, sessionId));
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
        operationalMetricsService.incrementLoginFailure();
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

    private String extractSessionIdFromRefreshToken(String refreshToken) {
        try {
            String sessionId = jwtTokenProvider.getSessionId(refreshToken);
            if (sessionId == null || sessionId.isBlank()) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            return sessionId;
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    private String refreshTokenKey(Long userId, String sessionId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + sessionId;
    }

    private String loginFailKey(String email) {
        return LOGIN_FAIL_KEY_PREFIX + normalizeLoginKeyIdentifier(email);
    }

    private String loginLockKey(String email) {
        return LOGIN_LOCK_KEY_PREFIX + normalizeLoginKeyIdentifier(email);
    }

    private String blackListKey(String accessTokenId) {
        return BLACKLIST_KEY_PREFIX + accessTokenId;
    }

    private String normalizeLoginKeyIdentifier(String email) {
        if (!StringUtils.hasText(email)) {
            return "unknown";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private LoginTokens issueLoginTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), sessionId);

        redisService.setValue(
                refreshTokenKey(user.getId(), sessionId),
                refreshToken,
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
        );

        return new LoginTokens(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration(),
                jwtProperties.getRefreshTokenExpiration(),
                AuthUserResponse.from(user)
        );
    }

    private User createOAuthUser(OAuthProvider provider, OAuthProfile profile) {
        userRepository.findByEmail(profile.email())
                .ifPresent(existingUser -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
                });

        User user = User.builder()
                .email(profile.email())
                .nickname(resolveOAuthNickname(profile.nickname(), profile.email()))
                .provider(provider.name())
                .providerId(profile.providerId())
                .role(Role.ROLE_USER)
                .build();
        return userRepository.save(user);
    }

    private void validateOAuthProfile(OAuthProfile profile) {
        if (!StringUtils.hasText(profile.providerId()) || !StringUtils.hasText(profile.email())) {
            throw new BusinessException(ErrorCode.OAUTH_PROFILE_INVALID);
        }
    }

    private String resolveOAuthNickname(String nickname, String email) {
        String resolved = nickname;
        if (!StringUtils.hasText(resolved)) {
            int atIndex = email.indexOf('@');
            resolved = atIndex > 0 ? email.substring(0, atIndex) : email;
        }

        String trimmed = resolved.trim();
        if (trimmed.length() > 20) {
            return trimmed.substring(0, 20);
        }
        return trimmed;
    }
}

