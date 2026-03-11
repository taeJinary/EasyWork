package com.taskflow.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.email-verification.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UnverifiedUserCleanupScheduler {

    private final UnverifiedUserCleanupService unverifiedUserCleanupService;

    @Value("${app.email-verification.cleanup.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.email-verification.cleanup.interval-ms:3600000}")
    public void cleanupExpiredUnverifiedUsers() {
        try {
            unverifiedUserCleanupService.cleanupExpiredUnverifiedUsers(batchSize);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to execute unverified user cleanup scheduler. batchSize={}",
                    batchSize,
                    exception
            );
        }
    }
}
