package com.taskflow.backend.domain.invitation.dto.response;

import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import java.time.LocalDateTime;

public record WorkspaceInvitationSummaryResponse(
        Long invitationId,
        Long workspaceId,
        Long inviteeUserId,
        String inviteeEmail,
        String inviteeNickname,
        WorkspaceRole role,
        InvitationStatus status,
        LocalDateTime expiresAt
) {
}
