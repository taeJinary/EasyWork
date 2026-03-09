package com.taskflow.backend.domain.invitation.dto.response;

import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import java.time.LocalDateTime;

public record WorkspaceInvitationListItemResponse(
        Long invitationId,
        Long workspaceId,
        String workspaceName,
        Long inviterUserId,
        String inviterNickname,
        WorkspaceRole role,
        InvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
