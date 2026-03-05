package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RetryQueuePropertiesValidator {

    private static final int MAX_MAINTENANCE_DELETE_BATCH_SIZE = 1000;
    private static final int MAX_RETRY_BATCH_SIZE = 500;

    private final long maintenanceIntervalMs;
    private final long maintenanceRetentionDays;
    private final long maintenancePendingWarnThreshold;
    private final int maintenanceDeleteBatchSize;
    private final int invitationEmailRetryMaxAttempts;
    private final int notificationPushRetryMaxAttempts;
    private final int attachmentCleanupRetryMaxAttempts;
    private final int invitationEmailRetryBatchSize;
    private final int notificationPushRetryBatchSize;
    private final int attachmentCleanupBatchSize;

    public RetryQueuePropertiesValidator(
            @Value("${app.retry-queue.maintenance.interval-ms:300000}") long maintenanceIntervalMs,
            @Value("${app.retry-queue.maintenance.retention-days:7}") long maintenanceRetentionDays,
            @Value("${app.retry-queue.maintenance.pending-warn-threshold:100}") long maintenancePendingWarnThreshold,
            @Value("${app.retry-queue.maintenance.delete-batch-size:500}") int maintenanceDeleteBatchSize,
            @Value("${app.invitation.email.retry.max-attempts:10}") int invitationEmailRetryMaxAttempts,
            @Value("${app.notification.push.retry.max-attempts:10}") int notificationPushRetryMaxAttempts,
            @Value("${app.attachment.cleanup.max-retry-attempts:10}") int attachmentCleanupRetryMaxAttempts,
            @Value("${app.invitation.email.retry.batch-size:50}") int invitationEmailRetryBatchSize,
            @Value("${app.notification.push.retry.batch-size:50}") int notificationPushRetryBatchSize,
            @Value("${app.attachment.cleanup.batch-size:50}") int attachmentCleanupBatchSize
    ) {
        this.maintenanceIntervalMs = maintenanceIntervalMs;
        this.maintenanceRetentionDays = maintenanceRetentionDays;
        this.maintenancePendingWarnThreshold = maintenancePendingWarnThreshold;
        this.maintenanceDeleteBatchSize = maintenanceDeleteBatchSize;
        this.invitationEmailRetryMaxAttempts = invitationEmailRetryMaxAttempts;
        this.notificationPushRetryMaxAttempts = notificationPushRetryMaxAttempts;
        this.attachmentCleanupRetryMaxAttempts = attachmentCleanupRetryMaxAttempts;
        this.invitationEmailRetryBatchSize = invitationEmailRetryBatchSize;
        this.notificationPushRetryBatchSize = notificationPushRetryBatchSize;
        this.attachmentCleanupBatchSize = attachmentCleanupBatchSize;
    }

    @PostConstruct
    public void validateAtStartup() {
        requirePositive(maintenanceIntervalMs, "app.retry-queue.maintenance.interval-ms");
        requirePositive(maintenanceRetentionDays, "app.retry-queue.maintenance.retention-days");
        requireNonNegative(maintenancePendingWarnThreshold, "app.retry-queue.maintenance.pending-warn-threshold");
        requirePositiveAndAtMost(
                maintenanceDeleteBatchSize,
                MAX_MAINTENANCE_DELETE_BATCH_SIZE,
                "app.retry-queue.maintenance.delete-batch-size"
        );
        requirePositive(invitationEmailRetryMaxAttempts, "app.invitation.email.retry.max-attempts");
        requirePositive(notificationPushRetryMaxAttempts, "app.notification.push.retry.max-attempts");
        requirePositive(attachmentCleanupRetryMaxAttempts, "app.attachment.cleanup.max-retry-attempts");
        requirePositiveAndAtMost(
                invitationEmailRetryBatchSize,
                MAX_RETRY_BATCH_SIZE,
                "app.invitation.email.retry.batch-size"
        );
        requirePositiveAndAtMost(
                notificationPushRetryBatchSize,
                MAX_RETRY_BATCH_SIZE,
                "app.notification.push.retry.batch-size"
        );
        requirePositiveAndAtMost(
                attachmentCleanupBatchSize,
                MAX_RETRY_BATCH_SIZE,
                "app.attachment.cleanup.batch-size"
        );
    }

    private void requirePositive(long value, String property) {
        if (value <= 0L) {
            throw new IllegalStateException(property + " must be greater than 0");
        }
    }

    private void requirePositive(int value, String property) {
        if (value <= 0) {
            throw new IllegalStateException(property + " must be greater than 0");
        }
    }

    private void requireNonNegative(long value, String property) {
        if (value < 0L) {
            throw new IllegalStateException(property + " must be greater than or equal to 0");
        }
    }

    private void requirePositiveAndAtMost(int value, int maxValue, String property) {
        requirePositive(value, property);
        if (value > maxValue) {
            throw new IllegalStateException(property + " must be less than or equal to " + maxValue);
        }
    }
}
