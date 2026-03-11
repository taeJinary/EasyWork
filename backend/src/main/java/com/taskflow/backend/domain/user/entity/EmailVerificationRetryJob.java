package com.taskflow.backend.domain.user.entity;

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
@Table(name = "email_verification_retry_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailVerificationRetryJob extends BaseEntity {

    private static final int LAST_ERROR_MESSAGE_MAX_LENGTH = 500;
    private static final int OPEN_KEY_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = OPEN_KEY_MAX_LENGTH, unique = true)
    private String openKey;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(length = LAST_ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    private LocalDateTime completedAt;

    public static EmailVerificationRetryJob createPending(
            Long userId,
            LocalDateTime nextRetryAt,
            String initialErrorMessage
    ) {
        return EmailVerificationRetryJob.builder()
                .userId(userId)
                .openKey(createOpenKey(userId))
                .retryCount(0)
                .nextRetryAt(nextRetryAt)
                .lastErrorMessage(truncateErrorMessage(initialErrorMessage))
                .build();
    }

    public void markCompleted(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        this.openKey = null;
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
        this.openKey = null;
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

    private static String createOpenKey(Long userId) {
        return String.valueOf(userId);
    }
}
