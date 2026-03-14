package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitPropertiesValidatorTest {

    @Test
    void validPropertiesPassValidation() {
        RateLimitPropertiesValidator validator = createValidator(validFixture());

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void nonPositiveMaxAttemptsFailsValidation() {
        Fixture fixture = validFixture();
        fixture.authLoginIpMaxAttempts = 0;

        assertThatThrownBy(createValidator(fixture)::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.ip.max-attempts");
    }

    @Test
    void nonPositiveWindowSecondsFailsValidation() {
        Fixture fixture = validFixture();
        fixture.authLoginWindowSeconds = 0;

        assertThatThrownBy(createValidator(fixture)::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.window-seconds");
    }

    @Test
    void tooHighMaxAttemptsFailsValidation() {
        Fixture fixture = validFixture();
        fixture.authLoginIpMaxAttempts = 10001;

        assertThatThrownBy(createValidator(fixture)::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.ip.max-attempts");
    }

    @Test
    void tooHighWindowSecondsFailsValidation() {
        Fixture fixture = validFixture();
        fixture.authLoginWindowSeconds = 86401;

        assertThatThrownBy(createValidator(fixture)::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.window-seconds");
    }

    @Test
    void nonPositiveInvitationIpMaxAttemptsFailsValidation() {
        Fixture fixture = validFixture();
        fixture.invitationCreateIpMaxAttempts = 0;

        assertThatThrownBy(createValidator(fixture)::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.invitation.create.ip.max-attempts");
    }

    @Test
    void nonPositiveCommentIpMaxAttemptsFailsValidation() {
        Fixture fixture = validFixture();
        fixture.commentCreateIpMaxAttempts = 0;

        assertThatThrownBy(createValidator(fixture)::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.comment.create.ip.max-attempts");
    }

    private RateLimitPropertiesValidator createValidator(Fixture fixture) {
        return new RateLimitPropertiesValidator(
                fixture.authLoginIpMaxAttempts,
                fixture.authLoginEmailMaxAttempts,
                fixture.authLoginWindowSeconds,
                fixture.authTokenReissueIpMaxAttempts,
                fixture.authTokenReissueWindowSeconds,
                fixture.authOauthCodeLoginIpMaxAttempts,
                fixture.authOauthCodeLoginWindowSeconds,
                fixture.invitationCreateIpMaxAttempts,
                fixture.invitationCreateIpWindowSeconds,
                fixture.invitationCreateUserMaxAttempts,
                fixture.invitationCreateWindowSeconds,
                fixture.commentCreateIpMaxAttempts,
                fixture.commentCreateIpWindowSeconds,
                fixture.commentCreateUserMaxAttempts,
                fixture.commentCreateWindowSeconds,
                fixture.attachmentUploadIpMaxAttempts,
                fixture.attachmentUploadIpWindowSeconds,
                fixture.attachmentUploadUserMaxAttempts,
                fixture.attachmentUploadWindowSeconds,
                fixture.pushTokenRegisterIpMaxAttempts,
                fixture.pushTokenRegisterIpWindowSeconds,
                fixture.pushTokenRegisterUserMaxAttempts,
                fixture.pushTokenRegisterWindowSeconds
        );
    }

    private Fixture validFixture() {
        return new Fixture();
    }

    private static final class Fixture {
        int authLoginIpMaxAttempts = 30;
        int authLoginEmailMaxAttempts = 10;
        long authLoginWindowSeconds = 60;
        int authTokenReissueIpMaxAttempts = 60;
        long authTokenReissueWindowSeconds = 60;
        int authOauthCodeLoginIpMaxAttempts = 30;
        long authOauthCodeLoginWindowSeconds = 60;
        int invitationCreateIpMaxAttempts = 40;
        long invitationCreateIpWindowSeconds = 60;
        int invitationCreateUserMaxAttempts = 20;
        long invitationCreateWindowSeconds = 60;
        int commentCreateIpMaxAttempts = 60;
        long commentCreateIpWindowSeconds = 60;
        int commentCreateUserMaxAttempts = 60;
        long commentCreateWindowSeconds = 60;
        int attachmentUploadIpMaxAttempts = 20;
        long attachmentUploadIpWindowSeconds = 60;
        int attachmentUploadUserMaxAttempts = 20;
        long attachmentUploadWindowSeconds = 60;
        int pushTokenRegisterIpMaxAttempts = 30;
        long pushTokenRegisterIpWindowSeconds = 60;
        int pushTokenRegisterUserMaxAttempts = 30;
        long pushTokenRegisterWindowSeconds = 60;
    }
}
