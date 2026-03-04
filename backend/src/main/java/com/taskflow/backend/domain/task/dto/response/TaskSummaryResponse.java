package com.taskflow.backend.domain.task.dto.response;

import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;

public record TaskSummaryResponse(
        Long taskId,
        Long projectId,
        String title,
        TaskStatus status,
        TaskPriority priority,
        Integer position,
        Long version,
        AssigneeResponse assignee
) {
    public record AssigneeResponse(
            Long userId,
            String nickname
    ) {
    }
}
