package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthCodeLoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthLoginRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.entity.EmailVerificationToken;
import com.taskflow.backend.domain.user.entity.PasswordHistory;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationTokenRepository;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.user.service.EmailVerificationTokenGenerator;
import com.taskflow.backend.domain.user.service.oauth.OAuthClient;
import com.taskflow.backend.domain.user.service.oauth.OAuthClientRegistry;
import com.taskflow.backend.domain.user.service.oauth.OAuthAccessTokenExchanger;
import com.taskflow.backend.domain.user.service.oauth.OAuthProfile;
import com.taskflow.backend.domain.user.service.model.LoginTokens;
import com.taskflow.backend.domain.user.service.model.ReissueTokens;
import com.taskflow.backend.global.auth.jwt.JwtProperties;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import com.taskflow.backend.infra.redis.RedisService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RedisService redisService;

    @Mock
    private OAuthClientRegistry oauthClientRegistry;

    @Mock
    private OAuthClient oauthClient;

    @Mock
    private OAuthAccessTokenExchanger oauthAccessTokenExchanger;

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @Mock
    private EmailVerificationTokenGenerator emailVerificationTokenGenerator;

    @Mock
    private EmailVerificationMailService emailVerificationMailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L);
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(1209600000L);
    }

    @Test
    void signupCreatesUserWhenEmailIsUnique() {
        SignupRequest request = new SignupRequest("new@example.com", "Pass123!", "newbie");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .nickname(user.getNickname())
                    .provider(user.getProvider())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .emailVerifiedAt(user.getEmailVerifiedAt())
                    .build();
        });
        when(emailVerificationTokenGenerator.generate()).thenReturn("raw-email-token");

        SignupResponse response = authService.signup(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.nickname()).isEqualTo("newbie");
        assertThat(response.emailVerificationRequired()).isTrue();
        verify(passwordHistoryRepository).save(any(PasswordHistory.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailVerificationMailService).sendVerificationEmail("new@example.com", "raw-email-token");
    }

    @Test
    void signupThrowsWhenEmailAlreadyExists() {
        SignupRequest request = new SignupRequest("dup@example.com", "Pass123!", "dup");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void loginReturnsTokensWhenCredentialsAreValid() {
        User user = activeUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "Pass123!");

        when(redisService.hasKey("login:lock:" + user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(org.mockito.ArgumentMatchers.eq(user.getId()), anyString()))
                .thenReturn("refresh-token");

        LoginTokens response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(1800000L);
        assertThat(response.refreshTokenExpiresIn()).isEqualTo(1209600000L);
        assertThat(response.user().userId()).isEqualTo(user.getId());

        ArgumentCaptor<String> refreshKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).setValue(
                refreshKeyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("refresh-token"),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(1209600000L))
        );
        assertThat(refreshKeyCaptor.getValue()).startsWith("refresh:" + user.getId() + ":");
        verify(redisService).delete("login:fail:" + user.getEmail());
        verify(redisService).delete("login:lock:" + user.getEmail());
    }

    @Test
    void loginThrowsWhenLocalEmailIsNotVerified() {
        User user = unverifiedLocalUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "Pass123!");

        when(redisService.hasKey("login:lock:" + user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void loginIncrementsFailCountWhenPasswordDoesNotMatch() {
        User user = activeUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "wrong-password");

        when(redisService.hasKey("login:lock:" + user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);
        when(redisService.increment("login:fail:" + user.getEmail())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(operationalMetricsService).incrementLoginFailure();
        verify(redisService).expire("login:fail:" + user.getEmail(), Duration.ofMinutes(5));
        verify(redisService, never()).setValue(
                "login:lock:" + user.getEmail(),
                "LOCKED",
                Duration.ofMinutes(5)
        );
    }

    @Test
    void loginNormalizesEmailWhenApplyingLockAndFailKeys() {
        LoginRequest request = new LoginRequest("  USER@Example.com  ", "wrong-password");

        when(redisService.hasKey("login:lock:user@example.com")).thenReturn(false);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(redisService.increment("login:fail:user@example.com")).thenReturn(1L);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(redisService).hasKey("login:lock:user@example.com");
        verify(redisService).increment("login:fail:user@example.com");
        verify(redisService).expire("login:fail:user@example.com", Duration.ofMinutes(5));
    }

    @Test
    void loginLocksAccountWhenFailCountReachesFive() {
        User user = activeUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "wrong-password");

        when(redisService.hasKey("login:lock:" + user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);
        when(redisService.increment("login:fail:" + user.getEmail())).thenReturn(5L);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(redisService).setValue(
                "login:lock:" + user.getEmail(),
                "LOCKED",
                Duration.ofMinutes(5)
        );
    }

    @Test
    void reissueRotatesTokensWhenRefreshTokenIsValid() {
        User user = activeUser();
        String refreshToken = "old-refresh-token";

        when(jwtTokenProvider.getUserId(refreshToken)).thenReturn(user.getId());
        when(jwtTokenProvider.getSessionId(refreshToken)).thenReturn("sid-123");
        when(redisService.getValue("refresh:" + user.getId() + ":sid-123")).thenReturn(Optional.of("old-refresh-token"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole()))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(user.getId(), "sid-123")).thenReturn("new-refresh-token");

        ReissueTokens response = authService.reissue(refreshToken);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(1800000L);
        assertThat(response.refreshTokenExpiresIn()).isEqualTo(1209600000L);

        verify(redisService).setValue(
                "refresh:" + user.getId() + ":sid-123",
                "new-refresh-token",
                Duration.ofMillis(1209600000L)
        );
    }

    @Test
    void reissueThrowsWhenRefreshTokenDoesNotMatchStoredToken() {
        String refreshToken = "unknown-refresh-token";

        when(jwtTokenProvider.getUserId(refreshToken)).thenReturn(1L);
        when(jwtTokenProvider.getSessionId(refreshToken)).thenReturn("sid-123");
        when(redisService.getValue("refresh:1:sid-123")).thenReturn(Optional.of("different-refresh-token"));

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);

        verify(operationalMetricsService).incrementRefreshReissueFailure();
    }

    @Test
    void oauthLoginReturnsTokensForExistingProviderUser() {
        OAuthLoginRequest request = new OAuthLoginRequest(OAuthProvider.GOOGLE, "oauth-access-token");
        OAuthProfile profile = new OAuthProfile("google-123", "oauth@example.com", "oauth-user");
        User oauthUser = User.builder()
                .id(2L)
                .email("oauth@example.com")
                .nickname("oauth-user")
                .provider("GOOGLE")
                .providerId("google-123")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(oauthClientRegistry.getClient(OAuthProvider.GOOGLE)).thenReturn(oauthClient);
        when(oauthClient.fetchProfile("oauth-access-token")).thenReturn(profile);
        when(userRepository.findByProviderAndProviderId("GOOGLE", "google-123")).thenReturn(Optional.of(oauthUser));
        when(jwtTokenProvider.generateAccessToken(2L, "oauth@example.com", Role.ROLE_USER))
                .thenReturn("oauth-access-jwt");
        when(jwtTokenProvider.generateRefreshToken(org.mockito.ArgumentMatchers.eq(2L), anyString()))
                .thenReturn("oauth-refresh-token");

        LoginTokens response = authService.oauthLogin(request);

        assertThat(response.accessToken()).isEqualTo("oauth-access-jwt");
        assertThat(response.refreshToken()).isEqualTo("oauth-refresh-token");
        assertThat(response.user().email()).isEqualTo("oauth@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void oauthLoginCreatesUserWhenProviderUserDoesNotExist() {
        OAuthLoginRequest request = new OAuthLoginRequest(OAuthProvider.GOOGLE, "oauth-access-token");
        OAuthProfile profile = new OAuthProfile("google-123", "oauth@example.com", "oauth-user");

        when(oauthClientRegistry.getClient(OAuthProvider.GOOGLE)).thenReturn(oauthClient);
        when(oauthClient.fetchProfile("oauth-access-token")).thenReturn(profile);
        when(userRepository.findByProviderAndProviderId("GOOGLE", "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(2L)
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .provider(user.getProvider())
                    .providerId(user.getProviderId())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .emailVerifiedAt(user.getEmailVerifiedAt())
                    .build();
        });
        when(jwtTokenProvider.generateAccessToken(2L, "oauth@example.com", Role.ROLE_USER))
                .thenReturn("oauth-access-jwt");
        when(jwtTokenProvider.generateRefreshToken(org.mockito.ArgumentMatchers.eq(2L), anyString()))
                .thenReturn("oauth-refresh-token");

        LoginTokens response = authService.oauthLogin(request);

        assertThat(response.accessToken()).isEqualTo("oauth-access-jwt");
        assertThat(response.refreshToken()).isEqualTo("oauth-refresh-token");
        assertThat(response.user().email()).isEqualTo("oauth@example.com");
        verify(passwordHistoryRepository, never()).save(any(PasswordHistory.class));
    }

    @Test
    void verifyEmailMarksUserAsVerifiedAndConsumesToken() {
        User user = unverifiedLocalUser();
        String rawToken = "raw-email-token";
        String hashedToken = hash(rawToken);
        EmailVerificationToken token = EmailVerificationToken.create(
                user,
                hashedToken,
                LocalDateTime.now().plusHours(1)
        );

        when(emailVerificationTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(token));

        authService.verifyEmail(rawToken);

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(token.getConsumedAt()).isNotNull();
    }

    @Test
    void resendEmailVerificationRevokesExistingTokensAndStoresNewToken() {
        User user = unverifiedLocalUser();
        EmailVerificationToken existingToken = EmailVerificationToken.create(
                user,
                "old-hash",
                LocalDateTime.now().plusHours(1)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(user.getId()))
                .thenReturn(java.util.List.of(existingToken));
        when(emailVerificationTokenGenerator.generate()).thenReturn("new-raw-token");

        authService.resendEmailVerification(user.getEmail());

        assertThat(existingToken.getRevokedAt()).isNotNull();
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailVerificationMailService).sendVerificationEmail(user.getEmail(), "new-raw-token");
    }

    @Test
    void oauthLoginThrowsWhenEmailAlreadyExistsWithAnotherAccount() {
        OAuthLoginRequest request = new OAuthLoginRequest(OAuthProvider.GOOGLE, "oauth-access-token");
        OAuthProfile profile = new OAuthProfile("google-123", "user@example.com", "oauth-user");

        when(oauthClientRegistry.getClient(OAuthProvider.GOOGLE)).thenReturn(oauthClient);
        when(oauthClient.fetchProfile("oauth-access-token")).thenReturn(profile);
        when(userRepository.findByProviderAndProviderId("GOOGLE", "google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> authService.oauthLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void oauthCodeLoginExchangesAuthorizationCodeBeforeOAuthLogin() {
        OAuthCodeLoginRequest request = new OAuthCodeLoginRequest(OAuthProvider.GOOGLE, "auth-code", null, null);
        OAuthProfile profile = new OAuthProfile("google-123", "oauth@example.com", "oauth-user");
        User oauthUser = User.builder()
                .id(2L)
                .email("oauth@example.com")
                .nickname("oauth-user")
                .provider("GOOGLE")
                .providerId("google-123")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(oauthAccessTokenExchanger.exchange(OAuthProvider.GOOGLE, "auth-code", null, null))
                .thenReturn("oauth-access-token");
        when(oauthClientRegistry.getClient(OAuthProvider.GOOGLE)).thenReturn(oauthClient);
        when(oauthClient.fetchProfile("oauth-access-token")).thenReturn(profile);
        when(userRepository.findByProviderAndProviderId("GOOGLE", "google-123")).thenReturn(Optional.of(oauthUser));
        when(jwtTokenProvider.generateAccessToken(2L, "oauth@example.com", Role.ROLE_USER))
                .thenReturn("oauth-access-jwt");
        when(jwtTokenProvider.generateRefreshToken(org.mockito.ArgumentMatchers.eq(2L), anyString()))
                .thenReturn("oauth-refresh-token");

        LoginTokens response = authService.oauthCodeLogin(request);

        assertThat(response.accessToken()).isEqualTo("oauth-access-jwt");
        assertThat(response.refreshToken()).isEqualTo("oauth-refresh-token");
        verify(oauthAccessTokenExchanger).exchange(OAuthProvider.GOOGLE, "auth-code", null, null);
    }

    @Test
    void logoutBlacklistsAccessTokenAndDeletesStoredRefreshToken() {
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenId("access-token")).thenReturn("access-jti");
        when(jwtTokenProvider.getRemainingExpiration("access-token")).thenReturn(5000L);
        when(jwtTokenProvider.getUserId("refresh-token")).thenReturn(1L);
        when(jwtTokenProvider.getSessionId("refresh-token")).thenReturn("sid-123");

        authService.logout("access-token", "refresh-token");

        verify(redisService).setValue(
                "blacklist:access-jti",
                "logout",
                Duration.ofMillis(5000L)
        );
        verify(redisService).delete("refresh:1:sid-123");
    }

    private User activeUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
    }

    private User unverifiedLocalUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded-password")
                .nickname("tester")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(null)
                .build();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
