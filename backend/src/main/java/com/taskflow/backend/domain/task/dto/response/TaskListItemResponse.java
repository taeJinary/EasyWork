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
        Long commentCount,
        AssigneeResponse assignee
) {
    public TaskListItemResponse(
            Long taskId,
            String title,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            Integer position,
            Long version,
            AssigneeResponse assignee
    ) {
        this(taskId, title, status, priority, dueDate, position, version, 0L, assignee);
    }

    public record AssigneeResponse(
            Long userId,
            String nickname
    ) {
    }
}
