package com.taskflow.backend.domain.invitation.entity;

import com.taskflow.backend.global.common.entity.BaseEntity;
import com.taskflow.backend.global.common.enums.ProjectRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "invitation_email_retry_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationEmailRetryJob extends BaseEntity {

    private static final int LAST_ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long invitationId;

    @Column(nullable = false, length = 320)
    private String inviteeEmail;

    @Column(nullable = false, length = 100)
    private String projectName;

    @Column(nullable = false, length = 50)
    private String inviterNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(length = LAST_ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    private LocalDateTime completedAt;

    public static InvitationEmailRetryJob createPending(
            Long invitationId,
            String inviteeEmail,
            String projectName,
            String inviterNickname,
            ProjectRole role,
            LocalDateTime nextRetryAt,
            String initialErrorMessage
    ) {
        return InvitationEmailRetryJob.builder()
                .invitationId(invitationId)
                .inviteeEmail(inviteeEmail)
                .projectName(projectName)
                .inviterNickname(inviterNickname)
                .role(role)
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

    public void markDeadLetter(String errorMessage, LocalDateTime completedAt) {
        this.retryCount += 1;
        this.lastErrorMessage = truncateErrorMessage(errorMessage);
        this.completedAt = completedAt;
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
