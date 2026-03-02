package com.taskflow.backend.domain.invitation.event;

import com.taskflow.backend.global.common.enums.ProjectRole;

public record InvitationCreatedEvent(
        Long invitationId,
        String inviteeEmail,
        String projectName,
        String inviterNickname,
        ProjectRole role
) {
}

