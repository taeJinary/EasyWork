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
    private final int authOauthAuthorizeUrlIpMaxAttempts;
    private final long authOauthAuthorizeUrlWindowSeconds;
    private final int authTokenReissueIpMaxAttempts;
    private final long authTokenReissueWindowSeconds;
    private final int authOauthCodeLoginIpMaxAttempts;
    private final long authOauthCodeLoginWindowSeconds;
    private final int invitationCreateIpMaxAttempts;
    private final long invitationCreateIpWindowSeconds;
    private final int invitationCreateUserMaxAttempts;
    private final long invitationCreateWindowSeconds;
    private final int commentCreateIpMaxAttempts;
    private final long commentCreateIpWindowSeconds;
    private final int commentCreateUserMaxAttempts;
    private final long commentCreateWindowSeconds;
    private final int attachmentUploadIpMaxAttempts;
    private final long attachmentUploadIpWindowSeconds;
    private final int attachmentUploadUserMaxAttempts;
    private final long attachmentUploadWindowSeconds;
    private final int pushTokenRegisterIpMaxAttempts;
    private final long pushTokenRegisterIpWindowSeconds;
    private final int pushTokenRegisterUserMaxAttempts;
    private final long pushTokenRegisterWindowSeconds;

    public RateLimitPropertiesValidator(
            @Value("${app.rate-limit.auth.login.ip.max-attempts:30}") int authLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.login.email.max-attempts:10}") int authLoginEmailMaxAttempts,
            @Value("${app.rate-limit.auth.login.window-seconds:60}") long authLoginWindowSeconds,
            @Value("${app.rate-limit.auth.oauth-authorize-url.ip.max-attempts:30}") int authOauthAuthorizeUrlIpMaxAttempts,
            @Value("${app.rate-limit.auth.oauth-authorize-url.window-seconds:60}") long authOauthAuthorizeUrlWindowSeconds,
            @Value("${app.rate-limit.auth.token-reissue.ip.max-attempts:60}") int authTokenReissueIpMaxAttempts,
            @Value("${app.rate-limit.auth.token-reissue.window-seconds:60}") long authTokenReissueWindowSeconds,
            @Value("${app.rate-limit.auth.oauth-code-login.ip.max-attempts:30}") int authOauthCodeLoginIpMaxAttempts,
            @Value("${app.rate-limit.auth.oauth-code-login.window-seconds:60}") long authOauthCodeLoginWindowSeconds,
            @Value("${app.rate-limit.invitation.create.ip.max-attempts:60}") int invitationCreateIpMaxAttempts,
            @Value("${app.rate-limit.invitation.create.ip.window-seconds:60}") long invitationCreateIpWindowSeconds,
            @Value("${app.rate-limit.invitation.create.user.max-attempts:20}") int invitationCreateUserMaxAttempts,
            @Value("${app.rate-limit.invitation.create.window-seconds:60}") long invitationCreateWindowSeconds,
            @Value("${app.rate-limit.comment.create.ip.max-attempts:60}") int commentCreateIpMaxAttempts,
            @Value("${app.rate-limit.comment.create.ip.window-seconds:60}") long commentCreateIpWindowSeconds,
            @Value("${app.rate-limit.comment.create.user.max-attempts:60}") int commentCreateUserMaxAttempts,
            @Value("${app.rate-limit.comment.create.window-seconds:60}") long commentCreateWindowSeconds,
            @Value("${app.rate-limit.attachment.upload.ip.max-attempts:20}") int attachmentUploadIpMaxAttempts,
            @Value("${app.rate-limit.attachment.upload.ip.window-seconds:60}") long attachmentUploadIpWindowSeconds,
            @Value("${app.rate-limit.attachment.upload.user.max-attempts:20}") int attachmentUploadUserMaxAttempts,
            @Value("${app.rate-limit.attachment.upload.window-seconds:60}") long attachmentUploadWindowSeconds,
            @Value("${app.rate-limit.notification.push-token-register.ip.max-attempts:30}") int pushTokenRegisterIpMaxAttempts,
            @Value("${app.rate-limit.notification.push-token-register.ip.window-seconds:60}") long pushTokenRegisterIpWindowSeconds,
            @Value("${app.rate-limit.notification.push-token-register.user.max-attempts:30}") int pushTokenRegisterUserMaxAttempts,
            @Value("${app.rate-limit.notification.push-token-register.window-seconds:60}") long pushTokenRegisterWindowSeconds
    ) {
        this.authLoginIpMaxAttempts = authLoginIpMaxAttempts;
        this.authLoginEmailMaxAttempts = authLoginEmailMaxAttempts;
        this.authLoginWindowSeconds = authLoginWindowSeconds;
        this.authOauthAuthorizeUrlIpMaxAttempts = authOauthAuthorizeUrlIpMaxAttempts;
        this.authOauthAuthorizeUrlWindowSeconds = authOauthAuthorizeUrlWindowSeconds;
        this.authTokenReissueIpMaxAttempts = authTokenReissueIpMaxAttempts;
        this.authTokenReissueWindowSeconds = authTokenReissueWindowSeconds;
        this.authOauthCodeLoginIpMaxAttempts = authOauthCodeLoginIpMaxAttempts;
        this.authOauthCodeLoginWindowSeconds = authOauthCodeLoginWindowSeconds;
        this.invitationCreateIpMaxAttempts = invitationCreateIpMaxAttempts;
        this.invitationCreateIpWindowSeconds = invitationCreateIpWindowSeconds;
        this.invitationCreateUserMaxAttempts = invitationCreateUserMaxAttempts;
        this.invitationCreateWindowSeconds = invitationCreateWindowSeconds;
        this.commentCreateIpMaxAttempts = commentCreateIpMaxAttempts;
        this.commentCreateIpWindowSeconds = commentCreateIpWindowSeconds;
        this.commentCreateUserMaxAttempts = commentCreateUserMaxAttempts;
        this.commentCreateWindowSeconds = commentCreateWindowSeconds;
        this.attachmentUploadIpMaxAttempts = attachmentUploadIpMaxAttempts;
        this.attachmentUploadIpWindowSeconds = attachmentUploadIpWindowSeconds;
        this.attachmentUploadUserMaxAttempts = attachmentUploadUserMaxAttempts;
        this.attachmentUploadWindowSeconds = attachmentUploadWindowSeconds;
        this.pushTokenRegisterIpMaxAttempts = pushTokenRegisterIpMaxAttempts;
        this.pushTokenRegisterIpWindowSeconds = pushTokenRegisterIpWindowSeconds;
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
                authOauthAuthorizeUrlIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.oauth-authorize-url.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                authTokenReissueIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.auth.token-reissue.ip.max-attempts"
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
                commentCreateIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.comment.create.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                commentCreateUserMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.comment.create.user.max-attempts"
        );
        requirePositiveAndAtMost(
                attachmentUploadIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.attachment.upload.ip.max-attempts"
        );
        requirePositiveAndAtMost(
                attachmentUploadUserMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.attachment.upload.user.max-attempts"
        );
        requirePositiveAndAtMost(
                pushTokenRegisterIpMaxAttempts,
                MAX_MAX_ATTEMPTS,
                "app.rate-limit.notification.push-token-register.ip.max-attempts"
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
                authOauthAuthorizeUrlWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.auth.oauth-authorize-url.window-seconds"
        );
        requirePositiveAndAtMost(
                authTokenReissueWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.auth.token-reissue.window-seconds"
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
                commentCreateIpWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.comment.create.ip.window-seconds"
        );
        requirePositiveAndAtMost(
                commentCreateWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.comment.create.window-seconds"
        );
        requirePositiveAndAtMost(
                attachmentUploadIpWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.attachment.upload.ip.window-seconds"
        );
        requirePositiveAndAtMost(
                attachmentUploadWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.attachment.upload.window-seconds"
        );
        requirePositiveAndAtMost(
                pushTokenRegisterIpWindowSeconds,
                MAX_WINDOW_SECONDS,
                "app.rate-limit.notification.push-token-register.ip.window-seconds"
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
