package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationMailPropertiesValidatorTest {

    @Test
    void skipsValidationWhenEmailVerificationIsDisabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                false,
                "noreply@example.com",
                "[TaskFlow]",
                "https://app.example.com/verify-email",
                true
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void skipsValidationOutsideProd() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "local"),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "http://localhost:5173/verify-email",
                false
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankFromAddressInProdWhenEnabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                " ",
                "[TaskFlow]",
                "https://app.example.com/verify-email",
                true
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.from");
    }

    @Test
    void rejectsWhitespacePaddedFromAddressInProdWhenEnabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                " noreply@example.com ",
                "[TaskFlow]",
                "https://app.example.com/verify-email",
                true
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.from");
    }

    @Test
    void rejectsNonHttpsVerificationUrlInProdWhenEnabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "http://app.example.com/verify-email",
                true
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.verify-base-url");
    }

    @Test
    void rejectsLocalhostVerificationUrlInProdWhenEnabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "https://localhost:5173/verify-email",
                true
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.verify-base-url");
    }

    @Test
    void rejectsLoopbackIpv4ShortcutVerificationUrlInProdWhenEnabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "https://127.1/verify-email",
                true
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.verify-base-url");
    }

    @Test
    void rejectsExpandedIpv6LoopbackVerificationUrlInProdWhenEnabled() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "https://[0:0:0:0:0:0:0:1]/verify-email",
                true
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.verify-base-url");
    }

    @Test
    void allowsNonLoopbackHostEvenWhenQueryContainsLocalhostText() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "https://app.example.com/verify-email?next=localhost",
                true
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void rejectsEnabledProdWhenMailSenderIsUnavailable() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "https://app.example.com/verify-email",
                false
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.mail");
    }

    @Test
    void acceptsValidProdConfiguration() {
        EmailVerificationMailPropertiesValidator validator = new EmailVerificationMailPropertiesValidator(
                prodEnvironment(),
                true,
                "noreply@example.com",
                "[TaskFlow]",
                "https://app.example.com/verify-email",
                true
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    private MockEnvironment prodEnvironment() {
        return new MockEnvironment().withProperty("spring.profiles.active", "prod");
    }
}
