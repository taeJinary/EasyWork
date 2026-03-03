package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.InvitationEmailRetryJob;
import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import com.taskflow.backend.domain.invitation.repository.InvitationEmailRetryJobRepository;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
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

    private final InvitationEmailRetryJobRepository invitationEmailRetryJobRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final InvitationEmailService invitationEmailService;

    @Value("${app.invitation.email.retry.delay-seconds:300}")
    private long retryDelaySeconds;

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
        if (shouldSkipRetry(job)) {
            job.markCompleted(LocalDateTime.now());
            invitationEmailRetryJobRepository.save(job);
            return;
        }

        InvitationCreatedEvent event = new InvitationCreatedEvent(
                job.getInvitationId(),
                job.getInviteeEmail(),
                job.getProjectName(),
                job.getInviterNickname(),
                job.getRole()
        );

        try {
            invitationEmailService.sendInvitationCreatedEmail(event);
            job.markCompleted(LocalDateTime.now());
            invitationEmailRetryJobRepository.save(job);
        } catch (Exception exception) {
            job.markFailed(
                    exception.getMessage(),
                    LocalDateTime.now().plusSeconds(retryDelaySeconds)
            );
            invitationEmailRetryJobRepository.save(job);

            log.error(
                    "Failed to retry invitation email send. invitationId={}, retryCount={}",
                    job.getInvitationId(),
                    job.getRetryCount(),
                    exception
            );
        }
    }

    private boolean shouldSkipRetry(InvitationEmailRetryJob job) {
        return projectInvitationRepository.findById(job.getInvitationId())
                .map(invitation -> !invitation.isPending() || invitation.getExpiresAt().isBefore(LocalDateTime.now()))
                .orElse(true);
    }
}
