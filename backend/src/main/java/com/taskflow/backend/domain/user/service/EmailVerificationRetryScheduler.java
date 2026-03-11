package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.global.ops.OperationalMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationRetryScheduler {

    private final EmailVerificationRetryService emailVerificationRetryService;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.email-verification.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.email-verification.retry.interval-ms:60000}")
    public void retryFailedEmails() {
        try {
            emailVerificationRetryService.retryPendingEmails(batchSize);
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementEmailVerificationRetryExecutionFailure();
            log.error(
                    "Failed to execute email verification retry scheduler. batchSize={}",
                    batchSize,
                    exception
            );
        }
    }
}
