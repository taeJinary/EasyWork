package com.taskflow.backend.domain.workspace.dto.response;

import com.taskflow.backend.global.common.enums.WorkspaceRole;

public record WorkspaceSummaryResponse(
        Long workspaceId,
        String name,
        String description,
        WorkspaceRole myRole
) {
}
