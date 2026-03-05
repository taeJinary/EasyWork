package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPropertiesValidator {

    private static final int MAX_MAX_ATTEMPTS = 10000;
    private static final long MAX_WINDOW_SECONDS = 86400L;

    private final int authLoginIpMaxAttempts;
    private final int authLoginEmailMaxAttempts;
    private final long authLoginWindowSeconds;
    private final int authTokenReissueIpMaxAttempts;
    private final long authTokenReissueWindowSeconds;
    private final int authOauthLoginIpMaxAttempts;
    private final long authOauthLoginWindowSeconds;
    private final int authOauthCodeLoginIpMaxAttempts;
    private final long authOauthCodeLoginWindowSeconds;
    private final int invitationCreateIpMaxAttempts;
    private final long invitationCreateIpWindowSeconds;
    private final int invitationCreateUserMaxAttempts;
    private final long invitationCreateWindowSeconds;
    private final int commentCreateUserMaxAttempts;
    private final long commentCreateWindowSeconds;
    private final int attachmentUploadUserMaxAttempts;
    private final long attachmentUploadWindowSeconds;
    private final int pushTokenRegisterUserMaxAttempts;
    private final long pushTokenRegisterWindowSeconds;

    public RateLimitPropertiesValidator(
            @Value("${app.rate-limit.auth.login.ip.max-attempts:30}") int authLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.login.email.max-attempts:10}") int authLoginEmailMaxAttempts,
            @Value("${app.rate-limit.auth.login.window-seconds:60}") long authLoginWindowSeconds,
            @Value("${app.rate-limit.auth.token-reissue.ip.max-attempts:60}") int authTokenReissueIpMaxAttempts,
            @Value("${app.rate-limit.auth.token-reissue.window-seconds:60}") long authTokenReissueWindowSeconds,
            @Value("${app.rate-limit.auth.oauth-login.ip.max-attempts:30}") int authOauthLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.oauth-login.window-seconds:60}") long authOauthLoginWindowSeconds,
            @Value("${app.rate-limit.auth.oauth-code-login.ip.max-attempts:30}") int authOauthCodeLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.oauth-code-login.window-seconds:60}") long authOauthCodeLoginWindowSeconds,
            @Value("${app.rate-limit.invitation.create.ip.max-attempts:60}") int invitationCreateIpMaxAttempts,
            @Value("${app.rate-limit.invitation.create.ip.window-seconds:60}") long invitationCreateIpWindowSeconds,
            @Value("${app.rate-limit.invitation.create.user.max-attempts:20}") int invitationCreateUserMaxAttempts,
            @Value("${app.rate-limit.invitation.create.window-seconds:60}") long invitationCreateWindowSeconds,
            @Value("${app.rate-limit.comment.create.user.max-attempts:60}") int commentCreateUserMaxAttempts,
            @Value("${app.rate-limit.comment.create.window-seconds:60}") long commentCreateWindowSeconds,
            @Value("${app.rate-limit.attachment.upload.user.max-attempts:20}") int attachmentUploadUserMaxAttempts,
            @Value("${app.rate-limit.attachment.upload.window-seconds:60}") long attachmentUploadWindowSeconds,
            @Value("${app.rate-limit.notification.push-token-register.user.max-attempts:30}") int pushTokenRegisterUserMaxAttempts,
            @Value("${app.rate-limit.notification.push-token-register.window-seconds:60}") long pushTokenRegisterWindowSeconds
    ) {
        this.authLoginIpMaxAttempts = authLoginIpMaxAttempts;
        this.authLoginEmailMaxAttempts = authLoginEmailMaxAttempts;
        this.authLoginWindowSeconds = authLoginWindowSeconds;
        this.authTokenReissueIpMaxAttempts = authTokenReissueIpMaxAttempts;
        this.authTokenReissueWindowSeconds = authTokenReissueWindowSeconds;
        this.authOauthLoginIpMaxAttempts = authOauthLoginIpMaxAttempts;
        this.authOauthLoginWindowSeconds = authOauthLoginWindowSeconds;
        this.authOauthCodeLoginIpMaxAttempts = authOauthCodeLoginIpMaxAttempts;
        this.authOauthCodeLoginWindowSeconds = authOauthCodeLoginWindowSeconds;
        this.invitationCreateIpMaxAttempts = invitationCreateIpMaxAttempts;
        this.invitationCreateIpWindowSeconds = invitationCreateIpWindowSeconds;
        this.invitationCreateUserMaxAttempts = invitationCreateUserMaxAttempts;
        this.invitationCreateWindowSeconds = invitationCreateWindowSeconds;
        this.commentCreateUserMaxAttempts = commentCreateUserMaxAttempts;
        this.commentCreateWindowSeconds = commentCreateWindowSeconds;
        this.attachmentUploadUserMaxAttempts = attachmentUploadUserMaxAttempts;
        this.attachmentUploadWindowSeconds = attachmentUploadWindowSeconds;
        this.pushTokenRegisterUserMaxAttempts = pushTokenRegisterUserMaxAttempts;
        this.pushTokenRegisterWindowSeconds = pushTokenRegisterWindowSeconds;
    }

    @PostConstruct
    public void validateAtStartup() {
        requirePositiveAndAtMost(
                authLoginIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.login.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                authLoginEmailMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.login.email.max-attempts"
        );
        requirePositiveAndAtMost(
                authTokenReissueIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.token-reissue.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                authOauthLoginIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.oauth-login.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                authOauthCodeLoginIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.oauth-code-login.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                invitationCreateIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.invitation.create.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                invitationCreateUserMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.invitation.create.user.max-attempts"
        );
        requirePositiveAndAtMost(
                commentCreateUserMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.comment.create.user.max-attempts"
        );
        requirePositiveAndAtMost(
                attachmentUploadUserMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.attachment.upload.user.max-attempts"
        );
        requirePositiveAndAtMost(
                pushTokenRegisterUserMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.notification.push-token-register.user.max-attempts"
        );

        requirePositiveAndAtMost(
                authLoginWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.auth.login.window-seconds"
        );
        requirePositiveAndAtMost(
                authTokenReissueWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.auth.token-reissue.window-seconds"
        );
        requirePositiveAndAtMost(
                authOauthLoginWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.auth.oauth-login.window-seconds"
        );
        requirePositiveAndAtMost(
                authOauthCodeLoginWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.auth.oauth-code-login.window-seconds"
        );
        requirePositiveAndAtMost(
                invitationCreateIpWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.invitation.create.ip.window-seconds"
        );
        requirePositiveAndAtMost(
                invitationCreateWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.invitation.create.window-seconds"
        );
        requirePositiveAndAtMost(
                commentCreateWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.comment.create.window-seconds"
        );
        requirePositiveAndAtMost(
                attachmentUploadWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.attachment.upload.window-seconds"
        );
        requirePositiveAndAtMost(
                pushTokenRegisterWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.notification.push-token-register.window-seconds"
        );
    }

    private void requirePositiveAndAtMost(int value, int maxValue, String property) {
        if (value <= 0) {
            throw new IllegalStateException(property + " must be greater than 0");
        }
        if (value > maxValue) {
            throw new IllegalStateException(property + " must be less than or equal to " + maxValue);
        }
    }

    private void requirePositiveAndAtMost(long value, long maxValue, String property) {
        if (value <= 0L) {
            throw new IllegalStateException(property + " must be greater than 0");
        }
        if (value > maxValue) {
            throw new IllegalStateException(property + " must be less than or equal to " + maxValue);
        }
    }
}
