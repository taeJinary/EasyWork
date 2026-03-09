package com.taskflow.backend.domain.invitation.dto.response;

import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;

public record WorkspaceInvitationActionResponse(
        Long invitationId,
        Long workspaceId,
        Long memberId,
        WorkspaceRole role,
        InvitationStatus status
) {
}
