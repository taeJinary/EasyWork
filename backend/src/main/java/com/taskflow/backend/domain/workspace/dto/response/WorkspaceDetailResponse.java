package com.taskflow.backend.domain.workspace.dto.response;

import com.taskflow.backend.global.common.enums.WorkspaceRole;
import java.time.LocalDateTime;

public record WorkspaceDetailResponse(
        Long workspaceId,
        String name,
        String description,
        WorkspaceRole myRole,
        long memberCount,
        LocalDateTime updatedAt
) {
}
