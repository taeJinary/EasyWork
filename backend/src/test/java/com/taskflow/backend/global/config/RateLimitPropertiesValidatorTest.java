package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitPropertiesValidatorTest {

    @Test
    void validPropertiesPassValidation() {
        RateLimitPropertiesValidator validator = new RateLimitPropertiesValidator(
                30,
                10,
                60,
                60,
                60,
                30,
                60,
                30,
                60,
                20,
                60,
                60,
                60,
                20,
                60,
                30,
                60
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void nonPositiveMaxAttemptsFailsValidation() {
        RateLimitPropertiesValidator validator = new RateLimitPropertiesValidator(
                0,
                10,
                60,
                60,
                60,
                30,
                60,
                30,
                60,
                20,
                60,
                60,
                60,
                20,
                60,
                30,
                60
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.ip.max-attempts");
    }

    @Test
    void nonPositiveWindowSecondsFailsValidation() {
        RateLimitPropertiesValidator validator = new RateLimitPropertiesValidator(
                30,
                10,
                0,
                60,
                60,
                30,
                60,
                30,
                60,
                20,
                60,
                60,
                60,
                20,
                60,
                30,
                60
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.window-seconds");
    }

    @Test
    void tooHighMaxAttemptsFailsValidation() {
        RateLimitPropertiesValidator validator = new RateLimitPropertiesValidator(
                10001,
                10,
                60,
                60,
                60,
                30,
                60,
                30,
                60,
                20,
                60,
                60,
                60,
                20,
                60,
                30,
                60
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.ip.max-attempts");
    }

    @Test
    void tooHighWindowSecondsFailsValidation() {
        RateLimitPropertiesValidator validator = new RateLimitPropertiesValidator(
                30,
                10,
                86401,
                60,
                60,
                30,
                60,
                30,
                60,
                20,
                60,
                60,
                60,
                20,
                60,
                30,
                60
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.rate-limit.auth.login.window-seconds");
    }
}
