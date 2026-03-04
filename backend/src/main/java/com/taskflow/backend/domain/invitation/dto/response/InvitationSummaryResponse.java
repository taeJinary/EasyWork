package com.taskflow.backend.domain.invitation.dto.response;

import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;

public record InvitationSummaryResponse(
        Long invitationId,
        Long projectId,
        Long inviteeUserId,
        String inviteeEmail,
        String inviteeNickname,
        ProjectRole role,
        InvitationStatus status,
        LocalDateTime expiresAt
) {
}

