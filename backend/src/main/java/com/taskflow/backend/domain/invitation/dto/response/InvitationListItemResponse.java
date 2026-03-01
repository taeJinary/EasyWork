package com.taskflow.backend.domain.invitation.dto.response;

import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;

public record InvitationListItemResponse(
        Long invitationId,
        Long projectId,
        String projectName,
        Long inviterUserId,
        String inviterNickname,
        ProjectRole role,
        InvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}

