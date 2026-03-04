package com.taskflow.backend.domain.dashboard.dto.response;

import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardProjectsResponse(
        long pendingInvitationCount,
        List<MyProjectResponse> myProjects
) {

    public record MyProjectResponse(
            Long projectId,
            String name,
            ProjectRole role,
            long memberCount,
            long taskCount,
            long doneTaskCount,
            int progressRate,
            LocalDateTime updatedAt
    ) {
    }
}
