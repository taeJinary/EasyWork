package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.entity.NotificationPushRetryJob;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
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

    private final NotificationPushRetryJobRepository notificationPushRetryJobRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPushTokenRepository notificationPushTokenRepository;
    private final NotificationPushDispatchService notificationPushDispatchService;

    @Value("${app.notification.push.retry.delay-seconds:300}")
    private long retryDelaySeconds;

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
        Notification notification = notificationRepository.findById(job.getNotificationId()).orElse(null);
        if (notification == null) {
            job.markCompleted(LocalDateTime.now());
            notificationPushRetryJobRepository.save(job);
            return;
        }
        NotificationPushToken pushToken = notificationPushTokenRepository.findById(job.getPushTokenId()).orElse(null);
        if (pushToken == null
                || !pushToken.isActive()
                || !pushToken.getUser().getId().equals(notification.getUser().getId())) {
            job.markCompleted(LocalDateTime.now());
            notificationPushRetryJobRepository.save(job);
            return;
        }

        try {
            boolean hasTransientFailure = notificationPushDispatchService.sendToToken(notification, pushToken);
            if (hasTransientFailure) {
                job.markFailed(
                        "Transient push delivery failure",
                        LocalDateTime.now().plusSeconds(retryDelaySeconds)
                );
            } else {
                job.markCompleted(LocalDateTime.now());
            }
            notificationPushRetryJobRepository.save(job);
        } catch (Exception exception) {
            job.markFailed(
                    exception.getMessage(),
                    LocalDateTime.now().plusSeconds(retryDelaySeconds)
            );
            notificationPushRetryJobRepository.save(job);
            log.error(
                    "Failed to retry push notification dispatch. notificationId={}, retryCount={}",
                    job.getNotificationId(),
                    job.getRetryCount(),
                    exception
            );
        }
    }

    private boolean isOpenJobDuplicateViolation(DataIntegrityViolationException exception) {
        Throwable rootCause = exception.getMostSpecificCause();
        String message = rootCause == null ? exception.getMessage() : rootCause.getMessage();
        return message != null && message.contains(OPEN_JOB_UNIQUE_KEY);
    }
}
