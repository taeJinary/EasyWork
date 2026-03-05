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
                500,
                10,
                10,
                10,
                50,
                50,
                50
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void nonPositiveIntervalFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                0L,
                7L,
                100L,
                500,
                10,
                10,
                10,
                50,
                50,
                50
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
                500,
                0,
                10,
                10,
                50,
                50,
                50
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.invitation.email.retry.max-attempts");
    }

    @Test
    void nonPositiveDeleteBatchSizeFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                0,
                10,
                10,
                10,
                50,
                50,
                50
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.retry-queue.maintenance.delete-batch-size");
    }

    @Test
    void excessiveMaintenanceDeleteBatchSizeFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                1001,
                10,
                10,
                10,
                50,
                50,
                50
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.retry-queue.maintenance.delete-batch-size");
    }

    @Test
    void excessiveInvitationRetryBatchSizeFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                500,
                10,
                10,
                10,
                1001,
                50,
                50
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.invitation.email.retry.batch-size");
    }

    @Test
    void excessiveNotificationRetryBatchSizeFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                500,
                10,
                10,
                10,
                50,
                1001,
                50
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.notification.push.retry.batch-size");
    }

    @Test
    void excessiveAttachmentCleanupBatchSizeFailsValidation() {
        RetryQueuePropertiesValidator validator = new RetryQueuePropertiesValidator(
                300000L,
                7L,
                100L,
                500,
                10,
                10,
                10,
                50,
                50,
                1001
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.attachment.cleanup.batch-size");
    }
}
