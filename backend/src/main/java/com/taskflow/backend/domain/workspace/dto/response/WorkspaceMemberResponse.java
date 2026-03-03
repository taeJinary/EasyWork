package com.taskflow.backend.domain.workspace.dto.response;

import com.taskflow.backend.global.common.enums.WorkspaceRole;
import java.time.LocalDateTime;

public record WorkspaceMemberResponse(
        Long memberId,
        Long userId,
        String email,
        String nickname,
        WorkspaceRole role,
        LocalDateTime joinedAt
) {
}
