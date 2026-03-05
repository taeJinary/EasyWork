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
    private final Counter invitationEmailRetryEnqueuedCounter;
    private final Counter invitationEmailRetryCompletedCounter;
    private final Counter invitationEmailRetryRescheduledCounter;
    private final Counter invitationEmailRetryDeadLetterCounter;
    private final Counter notificationPushRetryEnqueuedCounter;
    private final Counter notificationPushRetryCompletedCounter;
    private final Counter notificationPushRetryRescheduledCounter;
    private final Counter notificationPushRetryDeadLetterCounter;
    private final Counter attachmentCleanupRetryEnqueuedCounter;
    private final Counter attachmentCleanupRetryCompletedCounter;
    private final Counter attachmentCleanupRetryRescheduledCounter;
    private final Counter attachmentCleanupRetryDeadLetterCounter;
    private final Counter invitationRetryHistoryDeletedCounter;
    private final Counter notificationRetryHistoryDeletedCounter;
    private final Counter attachmentRetryHistoryDeletedCounter;

    private final AtomicLong invitationRetryBacklog = new AtomicLong(0L);
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
        this.invitationRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.invitation.history.deleted.total")
                .description("Total number of deleted invitation retry history rows")
                .register(meterRegistry);
        this.notificationRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.push.history.deleted.total")
                .description("Total number of deleted push retry history rows")
                .register(meterRegistry);
        this.attachmentRetryHistoryDeletedCounter = Counter.builder("taskflow.retry.attachment.history.deleted.total")
                .description("Total number of deleted attachment retry history rows")
                .register(meterRegistry);

        Gauge.builder("taskflow.retry.queue.backlog.invitation", invitationRetryBacklog, AtomicLong::get)
                .description("Pending invitation email retry jobs")
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

    public void updateRetryQueueBacklog(long invitationPending, long notificationPending, long attachmentPending) {
        invitationRetryBacklog.set(Math.max(invitationPending, 0L));
        notificationPushRetryBacklog.set(Math.max(notificationPending, 0L));
        attachmentCleanupRetryBacklog.set(Math.max(attachmentPending, 0L));
        totalRetryBacklog.set(Math.max(invitationPending + notificationPending + attachmentPending, 0L));
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

    public void recordRetryQueueHistoryDeleted(long invitationDeleted, long notificationDeleted, long attachmentDeleted) {
        if (invitationDeleted > 0L) {
            invitationRetryHistoryDeletedCounter.increment((double) invitationDeleted);
        }
        if (notificationDeleted > 0L) {
            notificationRetryHistoryDeletedCounter.increment((double) notificationDeleted);
        }
        if (attachmentDeleted > 0L) {
            attachmentRetryHistoryDeletedCounter.increment((double) attachmentDeleted);
        }
    }
}
