package com.taskflow.backend.domain.attachment.entity;

import com.taskflow.backend.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_attachment_cleanup_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskAttachmentCleanupJob extends BaseEntity {

    private static final int LAST_ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long attachmentId;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(length = LAST_ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    private LocalDateTime completedAt;

    public static TaskAttachmentCleanupJob createPending(
            Long attachmentId,
            String storagePath,
            LocalDateTime nextRetryAt
    ) {
        return TaskAttachmentCleanupJob.builder()
                .attachmentId(attachmentId)
                .storagePath(storagePath)
                .retryCount(0)
                .nextRetryAt(nextRetryAt)
                .build();
    }

    public void markCompleted(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage, LocalDateTime nextRetryAt) {
        this.retryCount += 1;
        this.lastErrorMessage = truncateErrorMessage(errorMessage);
        this.nextRetryAt = nextRetryAt;
    }

    public void markDeadLetter(String errorMessage, LocalDateTime completedAt) {
        this.retryCount += 1;
        this.lastErrorMessage = truncateErrorMessage(errorMessage);
        this.completedAt = completedAt;
    }

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= LAST_ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, LAST_ERROR_MESSAGE_MAX_LENGTH);
    }
}
