package com.taskflow.backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPushRetryScheduler {

    private final NotificationPushRetryService notificationPushRetryService;

    @Value("${app.notification.push.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notification.push.retry.interval-ms:60000}")
    public void retryFailedPushes() {
        notificationPushRetryService.retryPendingPushes(batchSize);
    }
}
