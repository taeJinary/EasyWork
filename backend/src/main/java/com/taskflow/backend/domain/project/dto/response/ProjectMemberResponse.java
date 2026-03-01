package com.taskflow.backend.domain.project.dto.response;

import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;

public record ProjectMemberResponse(
        Long memberId,
        Long userId,
        String email,
        String nickname,
        ProjectRole role,
        LocalDateTime joinedAt
) {
}

