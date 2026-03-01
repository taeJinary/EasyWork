package com.taskflow.backend.domain.project.dto.response;

import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResponse(
        Long projectId,
        String name,
        String description,
        ProjectRole myRole,
        Long memberCount,
        Long pendingInvitationCount,
        TaskSummaryResponse taskSummary,
        List<MemberResponse> members
) {
    public record TaskSummaryResponse(
            Long todo,
            Long inProgress,
            Long done
    ) {
    }

    public record MemberResponse(
            Long memberId,
            Long userId,
            String email,
            String nickname,
            ProjectRole role,
            LocalDateTime joinedAt
    ) {
    }
}

