package com.taskflow.backend.global.security;

import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ApiRateLimitService {

    private static final String KEY_PREFIX = "rate-limit";
    private static final String UNKNOWN = "unknown";

    private final RedisService redisService;

    private final int authLoginIpMaxAttempts;
    private final int authLoginEmailMaxAttempts;
    private final long authLoginWindowSeconds;
    private final int authTokenReissueIpMaxAttempts;
    private final long authTokenReissueWindowSeconds;
    private final int authOauthLoginIpMaxAttempts;
    private final long authOauthLoginWindowSeconds;
    private final int authOauthCodeLoginIpMaxAttempts;
    private final long authOauthCodeLoginWindowSeconds;
    private final int invitationCreateUserMaxAttempts;
    private final long invitationCreateWindowSeconds;
    private final int commentCreateUserMaxAttempts;
    private final long commentCreateWindowSeconds;
    private final int attachmentUploadUserMaxAttempts;
    private final long attachmentUploadWindowSeconds;
    private final int pushTokenRegisterUserMaxAttempts;
    private final long pushTokenRegisterWindowSeconds;

    public ApiRateLimitService(
            RedisService redisService,
            @Value("${app.rate-limit.auth.login.ip.max-attempts:30}") int authLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.login.email.max-attempts:10}") int authLoginEmailMaxAttempts,
            @Value("${app.rate-limit.auth.login.window-seconds:60}") long authLoginWindowSeconds,
            @Value("${app.rate-limit.auth.token-reissue.ip.max-attempts:60}") int authTokenReissueIpMaxAttempts,
            @Value("${app.rate-limit.auth.token-reissue.window-seconds:60}") long authTokenReissueWindowSeconds,
            @Value("${app.rate-limit.auth.oauth-login.ip.max-attempts:30}") int authOauthLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.oauth-login.window-seconds:60}") long authOauthLoginWindowSeconds,
            @Value("${app.rate-limit.auth.oauth-code-login.ip.max-attempts:30}") int authOauthCodeLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.oauth-code-login.window-seconds:60}") long authOauthCodeLoginWindowSeconds,
            @Value("${app.rate-limit.invitation.create.user.max-attempts:20}") int invitationCreateUserMaxAttempts,
            @Value("${app.rate-limit.invitation.create.window-seconds:60}") long invitationCreateWindowSeconds,
            @Value("${app.rate-limit.comment.create.user.max-attempts:60}") int commentCreateUserMaxAttempts,
            @Value("${app.rate-limit.comment.create.window-seconds:60}") long commentCreateWindowSeconds,
            @Value("${app.rate-limit.attachment.upload.user.max-attempts:20}") int attachmentUploadUserMaxAttempts,
            @Value("${app.rate-limit.attachment.upload.window-seconds:60}") long attachmentUploadWindowSeconds,
            @Value("${app.rate-limit.notification.push-token-register.user.max-attempts:30}") int pushTokenRegisterUserMaxAttempts,
            @Value("${app.rate-limit.notification.push-token-register.window-seconds:60}") long pushTokenRegisterWindowSeconds
    ) {
        this.redisService = redisService;
        this.authLoginIpMaxAttempts = authLoginIpMaxAttempts;
        this.authLoginEmailMaxAttempts = authLoginEmailMaxAttempts;
        this.authLoginWindowSeconds = authLoginWindowSeconds;
        this.authTokenReissueIpMaxAttempts = authTokenReissueIpMaxAttempts;
        this.authTokenReissueWindowSeconds = authTokenReissueWindowSeconds;
        this.authOauthLoginIpMaxAttempts = authOauthLoginIpMaxAttempts;
        this.authOauthLoginWindowSeconds = authOauthLoginWindowSeconds;
        this.authOauthCodeLoginIpMaxAttempts = authOauthCodeLoginIpMaxAttempts;
        this.authOauthCodeLoginWindowSeconds = authOauthCodeLoginWindowSeconds;
        this.invitationCreateUserMaxAttempts = invitationCreateUserMaxAttempts;
        this.invitationCreateWindowSeconds = invitationCreateWindowSeconds;
        this.commentCreateUserMaxAttempts = commentCreateUserMaxAttempts;
        this.commentCreateWindowSeconds = commentCreateWindowSeconds;
        this.attachmentUploadUserMaxAttempts = attachmentUploadUserMaxAttempts;
        this.attachmentUploadWindowSeconds = attachmentUploadWindowSeconds;
        this.pushTokenRegisterUserMaxAttempts = pushTokenRegisterUserMaxAttempts;
        this.pushTokenRegisterWindowSeconds = pushTokenRegisterWindowSeconds;
    }

    public void checkAuthLogin(HttpServletRequest request, String email) {
        String clientIp = resolveClientIp(request);
        enforce("auth:login:ip", clientIp, authLoginIpMaxAttempts, authLoginWindowSeconds);
        enforce("auth:login:email", normalizeEmail(email), authLoginEmailMaxAttempts, authLoginWindowSeconds);
    }

    public void checkAuthTokenReissue(HttpServletRequest request) {
        enforce(
                "auth:token-reissue:ip",
                resolveClientIp(request),
                authTokenReissueIpMaxAttempts,
                authTokenReissueWindowSeconds
        );
    }

    public void checkAuthOauthLogin(HttpServletRequest request) {
        enforce(
                "auth:oauth-login:ip",
                resolveClientIp(request),
                authOauthLoginIpMaxAttempts,
                authOauthLoginWindowSeconds
        );
    }

    public void checkAuthOauthCodeLogin(HttpServletRequest request) {
        enforce(
                "auth:oauth-code-login:ip",
                resolveClientIp(request),
                authOauthCodeLoginIpMaxAttempts,
                authOauthCodeLoginWindowSeconds
        );
    }

    public void checkInvitationCreate(Long userId) {
        enforce(
                "invitation:create:user",
                normalizeUserId(userId),
                invitationCreateUserMaxAttempts,
                invitationCreateWindowSeconds
        );
    }

    public void checkCommentCreate(Long userId) {
        enforce(
                "comment:create:user",
                normalizeUserId(userId),
                commentCreateUserMaxAttempts,
                commentCreateWindowSeconds
        );
    }

    public void checkAttachmentUpload(Long userId) {
        enforce(
                "attachment:upload:user",
                normalizeUserId(userId),
                attachmentUploadUserMaxAttempts,
                attachmentUploadWindowSeconds
        );
    }

    public void checkPushTokenRegister(Long userId) {
        enforce(
                "notification:push-token-register:user",
                normalizeUserId(userId),
                pushTokenRegisterUserMaxAttempts,
                pushTokenRegisterWindowSeconds
        );
    }

    private void enforce(String scope, String identifier, int maxAttempts, long windowSeconds) {
        String key = KEY_PREFIX + ":" + scope + ":" + identifier;
        Long count = redisService.increment(key);
        if (count == null) {
            log.warn("Rate limit increment returned null. scope={}", scope);
            return;
        }
        if (count == 1L) {
            redisService.expire(key, Duration.ofSeconds(windowSeconds));
        }
        if (count > maxAttempts) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        return StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : UNKNOWN;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return UNKNOWN;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUserId(Long userId) {
        return userId == null ? UNKNOWN : String.valueOf(userId);
    }
}
