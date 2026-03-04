package com.taskflow.backend.global.ops;

import com.taskflow.backend.domain.attachment.repository.TaskAttachmentCleanupJobRepository;
import com.taskflow.backend.domain.invitation.repository.InvitationEmailRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryQueueMaintenanceService {

    private final InvitationEmailRetryJobRepository invitationEmailRetryJobRepository;
    private final NotificationPushRetryJobRepository notificationPushRetryJobRepository;
    private final TaskAttachmentCleanupJobRepository taskAttachmentCleanupJobRepository;

    @Value("${app.retry-queue.maintenance.enabled:true}")
    private boolean enabled;

    @Value("${app.retry-queue.maintenance.retention-days:7}")
    private long retentionDays;

    @Value("${app.retry-queue.maintenance.pending-warn-threshold:100}")
    private long pendingWarnThreshold;

    @Value("${app.retry-queue.maintenance.delete-batch-size:500}")
    private int deleteBatchSize;

    @Scheduled(fixedDelayString = "${app.retry-queue.maintenance.interval-ms:300000}")
    @Transactional
    public void maintain() {
        if (!enabled) {
            return;
        }

        long invitationPending = invitationEmailRetryJobRepository.countByCompletedAtIsNull();
        long notificationPending = notificationPushRetryJobRepository.countByCompletedAtIsNull();
        long attachmentPending = taskAttachmentCleanupJobRepository.countByCompletedAtIsNull();
        long totalPending = invitationPending + notificationPending + attachmentPending;

        if (totalPending > 0L && totalPending >= pendingWarnThreshold) {
            log.warn(
                    "Retry queue backlog is high. invitationPending={}, notificationPending={}, attachmentPending={}, totalPending={}",
                    invitationPending,
                    notificationPending,
                    attachmentPending,
                    totalPending
            );
        } else if (totalPending > 0L) {
            log.info(
                    "Retry queue backlog snapshot. invitationPending={}, notificationPending={}, attachmentPending={}, totalPending={}",
                    invitationPending,
                    notificationPending,
                    attachmentPending,
                    totalPending
            );
        } else {
            log.debug("Retry queue backlog snapshot. totalPending=0");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long deletedInvitation = invitationEmailRetryJobRepository
                .deleteCompletedHistoryBefore(cutoff, deleteBatchSize);
        long deletedNotification = notificationPushRetryJobRepository
                .deleteCompletedHistoryBefore(cutoff, deleteBatchSize);
        long deletedAttachment = taskAttachmentCleanupJobRepository
                .deleteCompletedHistoryBefore(cutoff, deleteBatchSize);
        long totalDeleted = deletedInvitation + deletedNotification + deletedAttachment;

        if (totalDeleted > 0L) {
            log.info(
                    "Deleted expired retry queue history. invitationDeleted={}, notificationDeleted={}, attachmentDeleted={}, totalDeleted={}",
                    deletedInvitation,
                    deletedNotification,
                    deletedAttachment,
                    totalDeleted
            );
        }
    }
}
