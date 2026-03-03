package com.taskflow.backend.domain.attachment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskAttachmentCleanupRetryScheduler {

    private final TaskAttachmentCleanupRetryService cleanupRetryService;

    @Value("${app.attachment.cleanup.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.attachment.cleanup.retry-interval-ms:60000}")
    public void retryFailedDeletes() {
        cleanupRetryService.retryPendingDeletes(batchSize);
    }
}
