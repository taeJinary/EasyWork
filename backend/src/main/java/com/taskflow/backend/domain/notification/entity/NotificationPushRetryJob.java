package com.taskflow.backend.domain.notification.entity;

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
@Table(name = "notification_push_retry_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationPushRetryJob extends BaseEntity {

    private static final int LAST_ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long notificationId;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(length = LAST_ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    private LocalDateTime completedAt;

    public static NotificationPushRetryJob createPending(
            Long notificationId,
            LocalDateTime nextRetryAt,
            String initialErrorMessage
    ) {
        return NotificationPushRetryJob.builder()
                .notificationId(notificationId)
                .retryCount(0)
                .nextRetryAt(nextRetryAt)
                .lastErrorMessage(truncateErrorMessage(initialErrorMessage))
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

    private static String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= LAST_ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, LAST_ERROR_MESSAGE_MAX_LENGTH);
    }
}
