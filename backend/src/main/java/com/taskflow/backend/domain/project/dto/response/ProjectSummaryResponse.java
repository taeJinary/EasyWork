package com.taskflow.backend.domain.project.dto.response;

import com.taskflow.backend.global.common.enums.ProjectRole;

public record ProjectSummaryResponse(
        Long projectId,
        String name,
        String description,
        ProjectRole role
) {
}

