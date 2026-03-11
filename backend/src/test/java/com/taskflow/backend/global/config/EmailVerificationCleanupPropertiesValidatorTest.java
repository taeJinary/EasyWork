package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationCleanupPropertiesValidatorTest {

    @Test
    void validPropertiesPassValidation() {
        EmailVerificationCleanupPropertiesValidator validator =
                new EmailVerificationCleanupPropertiesValidator(3_600_000L, 24L, 100);

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void nonPositiveIntervalFailsValidation() {
        EmailVerificationCleanupPropertiesValidator validator =
                new EmailVerificationCleanupPropertiesValidator(0L, 24L, 100);

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.cleanup.interval-ms");
    }

    @Test
    void nonPositiveRetentionHoursFailValidation() {
        EmailVerificationCleanupPropertiesValidator validator =
                new EmailVerificationCleanupPropertiesValidator(3_600_000L, 0L, 100);

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.cleanup.retention-hours");
    }

    @Test
    void excessiveBatchSizeFailsValidation() {
        EmailVerificationCleanupPropertiesValidator validator =
                new EmailVerificationCleanupPropertiesValidator(3_600_000L, 24L, 501);

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.email-verification.cleanup.batch-size");
    }
}
