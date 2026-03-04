package com.taskflow.backend.domain.project.dto.response;

import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;

public record ProjectListItemResponse(
        Long projectId,
        String name,
        String description,
        ProjectRole role,
        Long memberCount,
        Long taskCount,
        Long doneTaskCount,
        int progressRate,
        LocalDateTime updatedAt
) {
}

