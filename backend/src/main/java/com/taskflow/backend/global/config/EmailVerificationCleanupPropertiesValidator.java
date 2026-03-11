package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationCleanupPropertiesValidator {

    private static final int MAX_BATCH_SIZE = 500;

    private final long intervalMs;
    private final long retentionHours;
    private final int batchSize;

    public EmailVerificationCleanupPropertiesValidator(
            @Value("${app.email-verification.cleanup.interval-ms:3600000}") long intervalMs,
            @Value("${app.email-verification.cleanup.retention-hours:24}") long retentionHours,
            @Value("${app.email-verification.cleanup.batch-size:100}") int batchSize
    ) {
        this.intervalMs = intervalMs;
        this.retentionHours = retentionHours;
        this.batchSize = batchSize;
    }

    @PostConstruct
    public void validateAtStartup() {
        requirePositive(intervalMs, "app.email-verification.cleanup.interval-ms");
        requirePositive(retentionHours, "app.email-verification.cleanup.retention-hours");
        requirePositiveAndAtMost(batchSize, MAX_BATCH_SIZE, "app.email-verification.cleanup.batch-size");
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

    private void requirePositiveAndAtMost(int value, int maxValue, String property) {
        requirePositive(value, property);
        if (value > maxValue) {
            throw new IllegalStateException(property + " must be less than or equal to " + maxValue);
        }
    }
}
