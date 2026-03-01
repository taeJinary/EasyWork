package com.taskflow.backend.domain.task.dto.response;

import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.time.LocalDate;

public record TaskListItemResponse(
        Long taskId,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
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
