package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.ops.OperationalMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushRetryScheduler {

    private final NotificationPushRetryService notificationPushRetryService;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.notification.push.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notification.push.retry.interval-ms:60000}")
    public void retryFailedPushes() {
        try {
            notificationPushRetryService.retryPendingPushes(batchSize);
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementNotificationPushRetryExecutionFailure();
            log.error("Failed to execute notification push retry scheduler. batchSize={}", batchSize, exception);
        }
    }
}
