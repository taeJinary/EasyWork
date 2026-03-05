package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.global.ops.OperationalMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAttachmentCleanupRetryScheduler {

    private final TaskAttachmentCleanupRetryService cleanupRetryService;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.attachment.cleanup.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.attachment.cleanup.retry-interval-ms:60000}")
    public void retryFailedDeletes() {
        try {
            cleanupRetryService.retryPendingDeletes(batchSize);
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementAttachmentCleanupRetryExecutionFailure();
            log.error("Failed to execute attachment cleanup retry scheduler. batchSize={}", batchSize, exception);
        }
    }
}
