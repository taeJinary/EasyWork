package com.taskflow.backend.global.ops;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OperationalMetricsService {

    private final Counter loginFailureCounter;
    private final Counter refreshReissueFailureCounter;
    private final Counter websocketConnectFailureCounter;
    private final Counter fileUploadFailureCounter;
    private final Counter retryQueueMaintenanceExecutionFailureCounter;
    private final Counter invitationEmailRetryEnqueuedCounter;
    private final Counter invitationEmailRetryCompletedCounter;
    private final Counter invitationEmailRetryRescheduledCounter;
    private final Counter invitationEmailRetryDeadLetterCounter;
    private final Counter invitationEmailRetryPersistenceFailureCounter;
    private final Counter invitationEmailRetryExecutionFailureCounter;
    private final Counter emailVerificationRetryEnqueuedCounter;
    private final Counter emailVerificationRetryCompletedCounter;
    private final Counter emailVerificationRetryRescheduledCounter;
    private final Counter emailVerificationRetryDeadLetterCounter;
    private final Counter emailVerificationRetryPersistenceFailureCounter;
    private final Counter emailVerificationRetryExecutionFailureCounter;
    private final Counter notificationPushRetryEnqueuedCounter;
    private final Counter notificationPushRetryCompletedCounter;
    private final Counter notificationPushRetryRescheduledCounter;
    private final Counter notificationPushRetryDeadLetterCounter;
    private final Counter notificationPushRetryPersistenceFailureCounter;
    private final Counter notificationPushRetryExecutionFailureCounter;
    private final Counter attachmentCleanupRetryEnqueuedCounter;
    private final Counter attachmentCleanupRetryCompletedCounter;
    private final Counter attachmentCleanupRetryRescheduledCounter;
    private final Counter attachmentCleanupRetryDeadLetterCounter;
    private final Counter attachmentCleanupRetryPersistenceFailureCounter;
    private final Counter attachmentCleanupRetryExecutionFailureCounter;
    private final Counter invitationRetryHistoryDeletedCounter;
    private final Counter notificationRetryHistoryDeletedCounter;
    private final Counter attachmentRetryHistoryDeletedCounter;
    private final Counter emailVerificationRetryHistoryDeletedCounter;

    private final AtomicLong invitationRetryBacklog = new AtomicLong(0L);
    private final AtomicLong emailVerificationRetryBacklog = new AtomicLong(0L);
    private final AtomicLong notificationPushRetryBacklog = new AtomicLong(0L);
    private final AtomicLong attachmentCleanupRetryBacklog = new AtomicLong(0L);
    private final AtomicLong totalRetryBacklog = new AtomicLong(0L);

    public OperationalMetricsService(MeterRegistry meterRegistry) {
        this.loginFailureCounter = Counter.builder("taskflow.auth.login.failure.total")
                .description("Total number of failed login attempts")
                .register(meterRegistry);
        this.refreshReissueFailureCounter = Counter.builder("taskflow.auth.refresh.reissue.failure.total")
                .description("Total number of failed refresh token reissue attempts")
                .register(meterRegistry);
        this.websocketConnectFailureCounter = Counter.builder("taskflow.websocket.connect.failure.total")
                .description("Total number of failed websocket CONNECT attempts")
                .register(meterRegistry);
        this.fileUploadFailureCounter = Counter.builder("taskflow.attachment.upload.failure.total")
                .description("Total number of failed task attachment uploads")
                .register(meterRegistry);
        this.retryQueueMaintenanceExecutionFailureCounter = Counter.builder("taskflow.retry.maintenance.execution.failure.total")
                .description("Total number of failed retry queue maintenance executions")
                .register(meterRegistry);
        this.invitationEmailRetryEnqueuedCounter = Counter.builder("taskflow.retry.invitation.enqueued.total")
                .description("Total number of enqueued invitation email retry jobs")
                .register(meterRegistry);
        this.invitationEmailRetryCompletedCounter = Counter.builder("taskflow.retry.invitation.completed.total")
                .description("Total number of completed invitation email retry jobs")
                .register(meterRegistry);
        this.invitationEmailRetryRescheduledCounter = Counter.builder("taskflow.retry.invitation.rescheduled.total")
                .description("Total number of rescheduled invitation email retry jobs")
                .register(meterRegistry);
        this.invitationEmailRetryDeadLetterCounter = Counter.builder("taskflow.retry.invitation.deadletter.total")
                .description("Total number of dead-lettered invitation email retry jobs")
                .register(meterRegistry);
        this.invitationEmailRetryPersistenceFailureCounter = Counter.builder("taskflow.retry.invitation.persistence.failure.total")
                .description("Total number of failed invitation email retry job persistence attempts")
                .register(meterRegistry);
        this.invitationEmailRetryExecutionFailureCounter = Counter.builder("taskflow.retry.invitation.execution.failure.total")
                .description("Total number of failed invitation email retry scheduler executions")
                .register(meterRegistry);
        this.emailVerificationRetryEnqueuedCounter = Counter.builder("taskflow.retry.email_verification.enqueued.total")
                .description("Total number of enqueued email verification retry jobs")
                .register(meterRegistry);
        this.emailVerificationRetryCompletedCounter = Counter.builder("taskflow.retry.email_verification.completed.total")
                .description("Total number of completed email verification retry jobs")
                .register(meterRegistry);
        this.emailVerificationRetryRescheduledCounter = Counter.builder("taskflow.retry.email_verification.rescheduled.total")
                .description("Total number of rescheduled email verification retry jobs")
                .register(meterRegistry);
        this.emailVerificationRetryDeadLetterCounter = Counter.builder("taskflow.retry.email_verification.deadletter.total")
                .description("Total number of dead-lettered email verification retry jobs")
                .register(meterRegistry);
        this.emailVerificationRetryPersistenceFailureCounter = Counter.builder("taskflow.retry.email_verification.persistence.failure.total")
                .description("Total number of failed email verification retry job persistence attempts")
                .register(meterRegistry);
        this.emailVerificationRetryExecutionFailureCounter = Counter.builder("taskflow.retry.email_verification.execution.failure.total")
                .description("Total number of failed email verification retry scheduler executions")
                .register(meterRegistry);
        this.notificationPushRetryEnqueuedCounter = Counter.builder("taskflow.retry.push.enqueued.total")
                .description("Total number of enqueued push retry jobs")
                .register(meterRegistry);
        this.notificationPushRetryCompletedCounter = Counter.builder("taskflow.retry.push.completed.total")
                .description("Total number of completed push retry jobs")
                .register(meterRegistry);
        this.notificationPushRetryRescheduledCounter = Counter.builder("taskflow.retry.push.rescheduled.total")
                .description("Total number of rescheduled push retry jobs")
                .register(meterRegistry);
        this.notificationPushRetryDeadLetterCounter = Counter.builder("taskflow.retry.push.deadletter.total")
                .description("Total number of dead-lettered push retry jobs")
                .register(meterRegistry);
        this.notificationPushRetryPersistenceFailureCounter = Counter.builder("taskflow.retry.push.persistence.failure.total")
                .description("Total number of failed push retry job persistence attempts")
                .register(meterRegistry);
        this.notificationPushRetryExecutionFailureCounter = Counter.builder("taskflow.retry.push.execution.failure.total")
                .description("Total number of failed push retry scheduler executions")
                .register(meterRegistry);
        this.attachmentCleanupRetryEnqueuedCounter = Counter.builder("taskflow.retry.attachment.enqueued.total")
                .description("Total number of enqueued attachment cleanup retry jobs")
                .register(meterRegistry);
        this.attachmentCleanupRetryCompletedCounter = Counter.builder("taskflow.retry.attachment.completed.total")
                .description("Total number of completed attachment cleanup retry jobs")
                .register(meterRegistry);
        this.attachmentCleanupRetryRescheduledCounter = Counter.builder("taskflow.retry.attachment.rescheduled.total")
                .description("Total number of rescheduled attachment cleanup retry jobs")
                .register(meterRegistry);
        this.attachmentCleanupRetryDeadLetterCounter = Counter.builder("taskflow.retry.attachment.deadletter.total")
                .description("Total number of dead-lettered attachment cleanup retry jobs")
                .register(meterRegistry);
        this.attachmentCleanupRetryPersistenceFailureCounter = Counter.builder("taskflow.retry.attachment.persistence.failure.total")
                .description("Total number of failed attachment cleanup retry job persistence attempts")
                .register(meterRegistry);
        this.attachmentCleanupRetryExecutionFailureCounter = Counter.builder("taskflow.retry.attachment.execution.failure.total")
                .description("Total number of failed attachment cleanup retry scheduler executions")
                .register(meterRegistry);
        this.invitationRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.invitation.history.deleted.total")
                .description("Total number of deleted invitation retry history rows")
                .register(meterRegistry);
        this.notificationRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.push.history.deleted.total")
                .description("Total number of deleted push retry history rows")
                .register(meterRegistry);
        this.attachmentRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.attachment.history.deleted.total")
                .description("Total number of deleted attachment retry history rows")
                .register(meterRegistry);
        this.emailVerificationRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.email_verification.history.deleted.total")
                .description("Total number of deleted email verification retry history rows")
                .register(meterRegistry);

        Gauge.builder("taskflow.retry.queue.backlog.invitation", invitationRetryBacklog, AtomicLong::get)
                .description("Pending invitation email retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.email_verification", emailVerificationRetryBacklog, AtomicLong::get)
                .description("Pending email verification retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.push", notificationPushRetryBacklog, AtomicLong::get)
                .description("Pending push retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.attachment", attachmentCleanupRetryBacklog, AtomicLong::get)
                .description("Pending attachment cleanup retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.total", totalRetryBacklog, AtomicLong::get)
                .description("Total pending retry jobs across all retry queues")
                .register(meterRegistry);
    }

    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }

    public void incrementRefreshReissueFailure() {
        refreshReissueFailureCounter.increment();
    }

    public void incrementWebSocketConnectFailure() {
        websocketConnectFailureCounter.increment();
    }

    public void incrementFileUploadFailure() {
        fileUploadFailureCounter.increment();
    }

    public void incrementRetryQueueMaintenanceExecutionFailure() {
        retryQueueMaintenanceExecutionFailureCounter.increment();
    }

    public void updateRetryQueueBacklog(
            long invitationPending,
            long emailVerificationPending,
            long notificationPending,
            long attachmentPending
    ) {
        invitationRetryBacklog.set(Math.max(invitationPending, 0L));
        emailVerificationRetryBacklog.set(Math.max(emailVerificationPending, 0L));
        notificationPushRetryBacklog.set(Math.max(notificationPending, 0L));
        attachmentCleanupRetryBacklog.set(Math.max(attachmentPending, 0L));
        totalRetryBacklog.set(Math.max(
                invitationPending + emailVerificationPending + notificationPending + attachmentPending,
                0L
        ));
    }

    public void incrementInvitationEmailRetryEnqueued() {
        invitationEmailRetryEnqueuedCounter.increment();
    }

    public void incrementInvitationEmailRetryCompleted() {
        invitationEmailRetryCompletedCounter.increment();
    }

    public void incrementInvitationEmailRetryRescheduled() {
        invitationEmailRetryRescheduledCounter.increment();
    }

    public void incrementInvitationEmailRetryDeadLetter() {
        invitationEmailRetryDeadLetterCounter.increment();
    }

    public void incrementInvitationEmailRetryPersistenceFailure() {
        invitationEmailRetryPersistenceFailureCounter.increment();
    }

    public void incrementInvitationEmailRetryExecutionFailure() {
        invitationEmailRetryExecutionFailureCounter.increment();
    }

    public void incrementEmailVerificationRetryEnqueued() {
        emailVerificationRetryEnqueuedCounter.increment();
    }

    public void incrementEmailVerificationRetryCompleted() {
        emailVerificationRetryCompletedCounter.increment();
    }

    public void incrementEmailVerificationRetryRescheduled() {
        emailVerificationRetryRescheduledCounter.increment();
    }

    public void incrementEmailVerificationRetryDeadLetter() {
        emailVerificationRetryDeadLetterCounter.increment();
    }

    public void incrementEmailVerificationRetryPersistenceFailure() {
        emailVerificationRetryPersistenceFailureCounter.increment();
    }

    public void incrementEmailVerificationRetryExecutionFailure() {
        emailVerificationRetryExecutionFailureCounter.increment();
    }

    public void incrementNotificationPushRetryEnqueued() {
        notificationPushRetryEnqueuedCounter.increment();
    }

    public void incrementNotificationPushRetryCompleted() {
        notificationPushRetryCompletedCounter.increment();
    }

    public void incrementNotificationPushRetryRescheduled() {
        notificationPushRetryRescheduledCounter.increment();
    }

    public void incrementNotificationPushRetryDeadLetter() {
        notificationPushRetryDeadLetterCounter.increment();
    }

    public void incrementNotificationPushRetryPersistenceFailure() {
        notificationPushRetryPersistenceFailureCounter.increment();
    }

    public void incrementNotificationPushRetryExecutionFailure() {
        notificationPushRetryExecutionFailureCounter.increment();
    }

    public void incrementAttachmentCleanupRetryEnqueued() {
        attachmentCleanupRetryEnqueuedCounter.increment();
    }

    public void incrementAttachmentCleanupRetryCompleted() {
        attachmentCleanupRetryCompletedCounter.increment();
    }

    public void incrementAttachmentCleanupRetryRescheduled() {
        attachmentCleanupRetryRescheduledCounter.increment();
    }

    public void incrementAttachmentCleanupRetryDeadLetter() {
        attachmentCleanupRetryDeadLetterCounter.increment();
    }

    public void incrementAttachmentCleanupRetryPersistenceFailure() {
        attachmentCleanupRetryPersistenceFailureCounter.increment();
    }

    public void incrementAttachmentCleanupRetryExecutionFailure() {
        attachmentCleanupRetryExecutionFailureCounter.increment();
    }

    public void recordRetryQueueHistoryDeleted(
            long invitationDeleted,
            long emailVerificationDeleted,
            long notificationDeleted,
            long attachmentDeleted
    ) {
        if (invitationDeleted > 0L) {
            invitationRetryHistoryDeletedCounter.increment((double) invitationDeleted);
        }
        if (emailVerificationDeleted > 0L) {
            emailVerificationRetryHistoryDeletedCounter.increment((double) emailVerificationDeleted);
        }
        if (notificationDeleted > 0L) {
            notificationRetryHistoryDeletedCounter.increment((double) notificationDeleted);
        }
        if (attachmentDeleted > 0L) {
            attachmentRetryHistoryDeletedCounter.increment((double) attachmentDeleted);
        }
    }
}
