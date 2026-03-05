package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.entity.NotificationPushRetryJob;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPushRetryService {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final String OPEN_JOB_UNIQUE_KEY = "uk_notification_push_retry_jobs_open_key";
    private static final int MAX_BACKOFF_SHIFT = 20;

    private final NotificationPushRetryJobRepository notificationPushRetryJobRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPushTokenRepository notificationPushTokenRepository;
    private final NotificationPushDispatchService notificationPushDispatchService;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.notification.push.retry.delay-seconds:300}")
    private long retryDelaySeconds;

    @Value("${app.notification.push.retry.max-attempts:10}")
    private int maxRetryAttempts;

    @Value("${app.notification.push.retry.max-delay-seconds:3600}")
    private long maxRetryDelaySeconds;

    @Transactional
    public void enqueueFailure(Notification notification, Long pushTokenId, String errorMessage) {
        Long notificationId = notification.getId();
        if (notificationId == null || pushTokenId == null) {
            return;
        }
        if (notificationPushRetryJobRepository
                .existsByNotificationIdAndPushTokenIdAndCompletedAtIsNull(notificationId, pushTokenId)) {
            return;
        }

        NotificationPushRetryJob job = NotificationPushRetryJob.createPending(
                notificationId,
                pushTokenId,
                LocalDateTime.now(),
                errorMessage
        );
        try {
            notificationPushRetryJobRepository.save(job);
            operationalMetricsService.incrementNotificationPushRetryEnqueued();
        } catch (DataIntegrityViolationException exception) {
            if (isOpenJobDuplicateViolation(exception)) {
                log.debug(
                        "Skipped duplicate open push retry job. notificationId={}, pushTokenId={}",
                        notificationId,
                        pushTokenId
                );
                return;
            }
            throw exception;
        }
    }

    @Transactional
    public void retryPendingPushes(int batchSize) {
        int normalizedBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        List<NotificationPushRetryJob> pendingJobs =
                notificationPushRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                        LocalDateTime.now(),
                        PageRequest.of(0, normalizedBatchSize)
                );

        for (NotificationPushRetryJob job : pendingJobs) {
            processRetryJob(job);
        }
    }

    private void processRetryJob(NotificationPushRetryJob job) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = notificationRepository.findById(job.getNotificationId()).orElse(null);
        if (notification == null) {
            job.markCompleted(now);
            notificationPushRetryJobRepository.save(job);
            operationalMetricsService.incrementNotificationPushRetryCompleted();
            return;
        }
        NotificationPushToken pushToken = notificationPushTokenRepository.findById(job.getPushTokenId()).orElse(null);
        if (pushToken == null
                || !pushToken.isActive()
                || !pushToken.getUser().getId().equals(notification.getUser().getId())) {
            job.markCompleted(now);
            notificationPushRetryJobRepository.save(job);
            operationalMetricsService.incrementNotificationPushRetryCompleted();
            return;
        }

        try {
            boolean hasTransientFailure = notificationPushDispatchService.sendToToken(notification, pushToken);
            if (hasTransientFailure) {
                markFailedOrDeadLetter(job, "Transient push delivery failure", now);
                if (job.getCompletedAt() != null) {
                    operationalMetricsService.incrementNotificationPushRetryDeadLetter();
                } else {
                    operationalMetricsService.incrementNotificationPushRetryRescheduled();
                }
            } else {
                job.markCompleted(now);
                operationalMetricsService.incrementNotificationPushRetryCompleted();
            }
            notificationPushRetryJobRepository.save(job);
        } catch (Exception exception) {
            markFailedOrDeadLetter(job, exception.getMessage(), now);
            notificationPushRetryJobRepository.save(job);
            if (job.getCompletedAt() != null) {
                operationalMetricsService.incrementNotificationPushRetryDeadLetter();
            } else {
                operationalMetricsService.incrementNotificationPushRetryRescheduled();
            }
            log.error(
                    "Failed to retry push notification dispatch. notificationId={}, retryCount={}",
                    job.getNotificationId(),
                    job.getRetryCount(),
                    exception
            );
        }
    }

    private void markFailedOrDeadLetter(NotificationPushRetryJob job, String errorMessage, LocalDateTime now) {
        int nextRetryCount = job.getRetryCount() + 1;
        if (nextRetryCount >= normalizeMaxRetryAttempts()) {
            job.markDeadLetter(errorMessage, now);
            log.warn(
                    "Push retry exhausted and marked completed. notificationId={}, pushTokenId={}, retryCount={}",
                    job.getNotificationId(),
                    job.getPushTokenId(),
                    job.getRetryCount()
            );
            return;
        }

        job.markFailed(errorMessage, now.plusSeconds(calculateDelaySeconds(nextRetryCount)));
    }

    private int normalizeMaxRetryAttempts() {
        return maxRetryAttempts > 0 ? maxRetryAttempts : 1;
    }

    private long calculateDelaySeconds(int nextRetryCount) {
        long baseDelaySeconds = Math.max(retryDelaySeconds, 1L);
        long maxDelaySeconds = Math.max(maxRetryDelaySeconds, baseDelaySeconds);
        int shift = Math.max(0, Math.min(nextRetryCount - 1, MAX_BACKOFF_SHIFT));
        long multiplier = 1L << shift;
        long computedDelay;
        try {
            computedDelay = Math.multiplyExact(baseDelaySeconds, multiplier);
        } catch (ArithmeticException exception) {
            computedDelay = Long.MAX_VALUE;
        }
        return Math.min(computedDelay, maxDelaySeconds);
    }

    private boolean isOpenJobDuplicateViolation(DataIntegrityViolationException exception) {
        Throwable rootCause = exception.getMostSpecificCause();
        String message = rootCause == null ? exception.getMessage() : rootCause.getMessage();
        return message != null && message.contains(OPEN_JOB_UNIQUE_KEY);
    }
}
