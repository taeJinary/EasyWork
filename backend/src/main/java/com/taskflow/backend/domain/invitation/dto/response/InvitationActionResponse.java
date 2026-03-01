package com.taskflow.backend.domain.invitation.dto.response;

import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;

public record InvitationActionResponse(
        Long invitationId,
        Long projectId,
        Long memberId,
        ProjectRole role,
        InvitationStatus status
) {
}

