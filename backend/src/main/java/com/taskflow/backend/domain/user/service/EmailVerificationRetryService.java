package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.entity.EmailVerificationRetryJob;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationRetryJobRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
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
public class EmailVerificationRetryService {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_BACKOFF_SHIFT = 20;

    private final EmailVerificationRetryJobRepository emailVerificationRetryJobRepository;
    private final UserRepository userRepository;
    private final EmailVerificationTokenManager emailVerificationTokenManager;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.email-verification.retry.delay-seconds:300}")
    private long retryDelaySeconds;

    @Value("${app.email-verification.retry.max-attempts:10}")
    private int maxRetryAttempts;

    @Value("${app.email-verification.retry.max-delay-seconds:3600}")
    private long maxRetryDelaySeconds;

    @Transactional
    public void enqueueFailure(Long userId, String errorMessage) {
        if (emailVerificationRetryJobRepository.existsByUserIdAndCompletedAtIsNull(userId)) {
            return;
        }

        EmailVerificationRetryJob job = EmailVerificationRetryJob.createPending(
                userId,
                LocalDateTime.now(),
                errorMessage
        );
        try {
            emailVerificationRetryJobRepository.save(job);
        } catch (DataIntegrityViolationException exception) {
            log.debug("Skipped duplicate email verification retry enqueue. userId={}", userId, exception);
            return;
        }
        operationalMetricsService.incrementEmailVerificationRetryEnqueued();
    }

    @Transactional
    public void retryPendingEmails(int batchSize) {
        int normalizedBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        List<EmailVerificationRetryJob> pendingJobs =
                emailVerificationRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                        LocalDateTime.now(),
                        PageRequest.of(0, normalizedBatchSize)
                );

        for (EmailVerificationRetryJob job : pendingJobs) {
            processJob(job);
        }
    }

    private void processJob(EmailVerificationRetryJob job) {
        LocalDateTime now = LocalDateTime.now();
        if (shouldSkipRetry(job.getUserId())) {
            job.markCompleted(now);
            saveJobAndRecordMetric(job, RetryOutcome.COMPLETED);
            return;
        }

        RetryOutcome outcome;
        try {
            User user = userRepository.findById(job.getUserId()).orElseThrow();
            emailVerificationTokenManager.reissue(user);
            job.markCompleted(now);
            outcome = RetryOutcome.COMPLETED;
        } catch (Exception exception) {
            markFailedOrDeadLetter(job, exception.getMessage(), now);
            outcome = determineRetryOutcome(job);

            log.error(
                    "Failed to retry email verification mail send. userId={}, retryCount={}",
                    job.getUserId(),
                    job.getRetryCount(),
                    exception
            );
        }

        saveJobAndRecordMetric(job, outcome);
    }

    private boolean shouldSkipRetry(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.isDeleted() || !user.isLocalAccount() || user.isEmailVerified())
                .orElse(true);
    }

    private void markFailedOrDeadLetter(EmailVerificationRetryJob job, String errorMessage, LocalDateTime now) {
        int nextRetryCount = job.getRetryCount() + 1;
        if (nextRetryCount >= normalizeMaxRetryAttempts()) {
            job.markDeadLetter(errorMessage, now);
            log.warn(
                    "Email verification retry exhausted and marked completed. userId={}, retryCount={}",
                    job.getUserId(),
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

    private RetryOutcome determineRetryOutcome(EmailVerificationRetryJob job) {
        return job.getCompletedAt() != null ? RetryOutcome.DEAD_LETTER : RetryOutcome.RESCHEDULED;
    }

    private void saveJobAndRecordMetric(EmailVerificationRetryJob job, RetryOutcome outcome) {
        try {
            emailVerificationRetryJobRepository.save(job);
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementEmailVerificationRetryPersistenceFailure();
            throw exception;
        }
        switch (outcome) {
            case COMPLETED -> operationalMetricsService.incrementEmailVerificationRetryCompleted();
            case RESCHEDULED -> operationalMetricsService.incrementEmailVerificationRetryRescheduled();
            case DEAD_LETTER -> operationalMetricsService.incrementEmailVerificationRetryDeadLetter();
        }
    }

    private enum RetryOutcome {
        COMPLETED,
        RESCHEDULED,
        DEAD_LETTER
    }
}
