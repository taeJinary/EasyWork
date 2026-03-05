package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.InvitationEmailRetryJob;
import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import com.taskflow.backend.domain.invitation.repository.InvitationEmailRetryJobRepository;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationEmailRetryService {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_BACKOFF_SHIFT = 20;

    private final InvitationEmailRetryJobRepository invitationEmailRetryJobRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final InvitationEmailService invitationEmailService;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.invitation.email.retry.delay-seconds:300}")
    private long retryDelaySeconds;

    @Value("${app.invitation.email.retry.max-attempts:10}")
    private int maxRetryAttempts;

    @Value("${app.invitation.email.retry.max-delay-seconds:3600}")
    private long maxRetryDelaySeconds;

    @Transactional
    public void enqueueFailure(InvitationCreatedEvent event, String errorMessage) {
        if (invitationEmailRetryJobRepository.existsByInvitationIdAndCompletedAtIsNull(event.invitationId())) {
            return;
        }

        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                event.invitationId(),
                event.inviteeEmail(),
                event.projectName(),
                event.inviterNickname(),
                event.role(),
                LocalDateTime.now(),
                errorMessage
        );
        invitationEmailRetryJobRepository.save(job);
        operationalMetricsService.incrementInvitationEmailRetryEnqueued();
    }

    @Transactional
    public void retryPendingEmails(int batchSize) {
        int normalizedBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        List<InvitationEmailRetryJob> pendingJobs =
                invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                        LocalDateTime.now(),
                        PageRequest.of(0, normalizedBatchSize)
                );

        for (InvitationEmailRetryJob job : pendingJobs) {
            processJob(job);
        }
    }

    private void processJob(InvitationEmailRetryJob job) {
        LocalDateTime now = LocalDateTime.now();
        if (shouldSkipRetry(job)) {
            job.markCompleted(now);
            saveJobAndRecordMetric(job, RetryOutcome.COMPLETED);
            return;
        }

        InvitationCreatedEvent event = new InvitationCreatedEvent(
                job.getInvitationId(),
                job.getInviteeEmail(),
                job.getProjectName(),
                job.getInviterNickname(),
                job.getRole()
        );

        RetryOutcome outcome;
        try {
            invitationEmailService.sendInvitationCreatedEmail(event);
            job.markCompleted(now);
            outcome = RetryOutcome.COMPLETED;
        } catch (Exception exception) {
            markFailedOrDeadLetter(job, exception.getMessage(), now);
            outcome = determineRetryOutcome(job);

            log.error(
                    "Failed to retry invitation email send. invitationId={}, retryCount={}",
                    job.getInvitationId(),
                    job.getRetryCount(),
                    exception
            );
        }

        saveJobAndRecordMetric(job, outcome);
    }

    private void markFailedOrDeadLetter(InvitationEmailRetryJob job, String errorMessage, LocalDateTime now) {
        int nextRetryCount = job.getRetryCount() + 1;
        if (nextRetryCount >= normalizeMaxRetryAttempts()) {
            job.markDeadLetter(errorMessage, now);
            log.warn(
                    "Invitation email retry exhausted and marked completed. invitationId={}, retryCount={}",
                    job.getInvitationId(),
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

    private boolean shouldSkipRetry(InvitationEmailRetryJob job) {
        return projectInvitationRepository.findById(job.getInvitationId())
                .map(invitation -> !invitation.isPending() || invitation.getExpiresAt().isBefore(LocalDateTime.now()))
                .orElse(true);
    }

    private RetryOutcome determineRetryOutcome(InvitationEmailRetryJob job) {
        return job.getCompletedAt() != null ? RetryOutcome.DEAD_LETTER : RetryOutcome.RESCHEDULED;
    }

    private void saveJobAndRecordMetric(InvitationEmailRetryJob job, RetryOutcome outcome) {
        invitationEmailRetryJobRepository.save(job);
        switch (outcome) {
            case COMPLETED -> operationalMetricsService.incrementInvitationEmailRetryCompleted();
            case RESCHEDULED -> operationalMetricsService.incrementInvitationEmailRetryRescheduled();
            case DEAD_LETTER -> operationalMetricsService.incrementInvitationEmailRetryDeadLetter();
        }
    }

    private enum RetryOutcome {
        COMPLETED,
        RESCHEDULED,
        DEAD_LETTER
    }
}
