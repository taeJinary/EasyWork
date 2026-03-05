package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.entity.TaskAttachmentCleanupJob;
import com.taskflow.backend.domain.attachment.repository.TaskAttachmentCleanupJobRepository;
import com.taskflow.backend.global.logging.SensitiveValueSanitizer;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAttachmentCleanupRetryService {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_BACKOFF_SHIFT = 20;

    private final TaskAttachmentCleanupJobRepository cleanupJobRepository;
    private final TaskAttachmentStorage taskAttachmentStorage;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.attachment.cleanup.retry-delay-seconds:300}")
    private long retryDelaySeconds;

    @Value("${app.attachment.cleanup.max-retry-attempts:10}")
    private int maxRetryAttempts;

    @Value("${app.attachment.cleanup.max-delay-seconds:3600}")
    private long maxRetryDelaySeconds;

    @Transactional
    public void enqueueDeleteFailure(Long attachmentId, String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }

        if (cleanupJobRepository.existsByStoragePathAndCompletedAtIsNull(storagePath)) {
            return;
        }

        TaskAttachmentCleanupJob job = TaskAttachmentCleanupJob.createPending(
                attachmentId,
                storagePath,
                LocalDateTime.now()
        );
        cleanupJobRepository.save(job);
        operationalMetricsService.incrementAttachmentCleanupRetryEnqueued();
    }

    @Transactional
    public void retryPendingDeletes(int batchSize) {
        int normalizedBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        List<TaskAttachmentCleanupJob> pendingJobs =
                cleanupJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                        LocalDateTime.now(),
                        PageRequest.of(0, normalizedBatchSize)
                );

        for (TaskAttachmentCleanupJob job : pendingJobs) {
            processDeleteJob(job);
        }
    }

    private void processDeleteJob(TaskAttachmentCleanupJob job) {
        LocalDateTime now = LocalDateTime.now();
        RetryOutcome outcome;
        try {
            taskAttachmentStorage.delete(job.getStoragePath());
            job.markCompleted(now);
            outcome = RetryOutcome.COMPLETED;
        } catch (Exception exception) {
            markFailedOrDeadLetter(job, exception.getMessage(), now);
            outcome = determineRetryOutcome(job);

            log.error(
                    "Failed to retry attachment cleanup delete. attachmentId={}, storagePathHash={}, retryCount={}",
                    job.getAttachmentId(),
                    SensitiveValueSanitizer.shortHash(job.getStoragePath()),
                    job.getRetryCount(),
                    exception
            );
        }

        saveJobAndRecordMetric(job, outcome);
    }

    private void markFailedOrDeadLetter(TaskAttachmentCleanupJob job, String errorMessage, LocalDateTime now) {
        int nextRetryCount = job.getRetryCount() + 1;
        if (nextRetryCount >= normalizeMaxRetryAttempts()) {
            job.markDeadLetter(errorMessage, now);
            log.warn(
                    "Attachment cleanup retry exhausted and marked completed. attachmentId={}, retryCount={}",
                    job.getAttachmentId(),
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

    private RetryOutcome determineRetryOutcome(TaskAttachmentCleanupJob job) {
        return job.getCompletedAt() != null ? RetryOutcome.DEAD_LETTER : RetryOutcome.RESCHEDULED;
    }

    private void saveJobAndRecordMetric(TaskAttachmentCleanupJob job, RetryOutcome outcome) {
        try {
            cleanupJobRepository.save(job);
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementAttachmentCleanupRetryPersistenceFailure();
            throw exception;
        }
        switch (outcome) {
            case COMPLETED -> operationalMetricsService.incrementAttachmentCleanupRetryCompleted();
            case RESCHEDULED -> operationalMetricsService.incrementAttachmentCleanupRetryRescheduled();
            case DEAD_LETTER -> operationalMetricsService.incrementAttachmentCleanupRetryDeadLetter();
        }
    }

    private enum RetryOutcome {
        COMPLETED,
        RESCHEDULED,
        DEAD_LETTER
    }
}
