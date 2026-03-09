package com.taskflow.backend.domain.invitation.entity;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.global.common.entity.BaseEntity;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "workspace_invitations",
        uniqueConstraints = @UniqueConstraint(name = "uk_workspace_invitation_pending_key", columnNames = "pending_key")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkspaceInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_user_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    private LocalDateTime respondedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "pending_key", unique = true, length = 100)
    private String pendingKey;

    public static WorkspaceInvitation create(
            Workspace workspace,
            User inviter,
            User invitee,
            WorkspaceRole role,
            InvitationStatus status,
            LocalDateTime expiresAt
    ) {
        return WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviter(inviter)
                .invitee(invitee)
                .role(role)
                .status(status)
                .expiresAt(expiresAt)
                .pendingKey(status == InvitationStatus.PENDING ? buildPendingKey(workspace, invitee) : null)
                .build();
    }

    public void accept(LocalDateTime respondedAt) {
        applyStatusChange(InvitationStatus.ACCEPTED, respondedAt);
    }

    public void reject(LocalDateTime respondedAt) {
        applyStatusChange(InvitationStatus.REJECTED, respondedAt);
    }

    public void cancel(LocalDateTime respondedAt) {
        applyStatusChange(InvitationStatus.CANCELED, respondedAt);
    }

    public void expire(LocalDateTime respondedAt) {
        applyStatusChange(InvitationStatus.EXPIRED, respondedAt);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    private void applyStatusChange(InvitationStatus nextStatus, LocalDateTime respondedAt) {
        this.status = nextStatus;
        this.respondedAt = respondedAt;
        if (nextStatus != InvitationStatus.PENDING) {
            this.pendingKey = null;
        }
    }

    private static String buildPendingKey(Workspace workspace, User invitee) {
        if (workspace.getId() == null || invitee.getId() == null) {
            return null;
        }
        return workspace.getId() + ":" + invitee.getId();
    }
}
