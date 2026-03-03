package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RetryQueuePropertiesValidator {

    private final long maintenanceIntervalMs;
    private final long maintenanceRetentionDays;
    private final long maintenancePendingWarnThreshold;
    private final int maintenanceDeleteBatchSize;
    private final int invitationEmailRetryMaxAttempts;
    private final int notificationPushRetryMaxAttempts;
    private final int attachmentCleanupRetryMaxAttempts;

    public RetryQueuePropertiesValidator(
            @Value("${app.retry-queue.maintenance.interval-ms:300000}") long maintenanceIntervalMs,
            @Value("${app.retry-queue.maintenance.retention-days:7}") long maintenanceRetentionDays,
            @Value("${app.retry-queue.maintenance.pending-warn-threshold:100}") long maintenancePendingWarnThreshold,
            @Value("${app.retry-queue.maintenance.delete-batch-size:500}") int maintenanceDeleteBatchSize,
            @Value("${app.invitation.email.retry.max-attempts:10}") int invitationEmailRetryMaxAttempts,
            @Value("${app.notification.push.retry.max-attempts:10}") int notificationPushRetryMaxAttempts,
            @Value("${app.attachment.cleanup.max-retry-attempts:10}") int attachmentCleanupRetryMaxAttempts
    ) {
        this.maintenanceIntervalMs = maintenanceIntervalMs;
        this.maintenanceRetentionDays = maintenanceRetentionDays;
        this.maintenancePendingWarnThreshold = maintenancePendingWarnThreshold;
        this.maintenanceDeleteBatchSize = maintenanceDeleteBatchSize;
        this.invitationEmailRetryMaxAttempts = invitationEmailRetryMaxAttempts;
        this.notificationPushRetryMaxAttempts = notificationPushRetryMaxAttempts;
        this.attachmentCleanupRetryMaxAttempts = attachmentCleanupRetryMaxAttempts;
    }

    @PostConstruct
    public void validateAtStartup() {
        requirePositive(maintenanceIntervalMs, "app.retry-queue.maintenance.interval-ms");
        requirePositive(maintenanceRetentionDays, "app.retry-queue.maintenance.retention-days");
        requireNonNegative(maintenancePendingWarnThreshold, "app.retry-queue.maintenance.pending-warn-threshold");
        requirePositive(maintenanceDeleteBatchSize, "app.retry-queue.maintenance.delete-batch-size");
        requirePositive(invitationEmailRetryMaxAttempts, "app.invitation.email.retry.max-attempts");
        requirePositive(notificationPushRetryMaxAttempts, "app.notification.push.retry.max-attempts");
        requirePositive(attachmentCleanupRetryMaxAttempts, "app.attachment.cleanup.max-retry-attempts");
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
}
