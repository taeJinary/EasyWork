package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryQueuePropertiesValidatorTest {

    @Test
    void validPropertiesPassValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                10,
                10,
                10
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void nonPositiveIntervalFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                0L,
                7L,
                100L,
                10,
                10,
                10
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.retry-queue.maintenance.interval-ms");
    }

    @Test
    void nonPositiveMaxAttemptsFailValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                0,
                10,
                10
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.invitation.email.retry.max-attempts");
    }
}
