package com.taskflow.backend.domain.task.dto.response;

import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;

public record TaskBoardResponse(
        Long projectId,
        FilterResponse filters,
        List<ColumnResponse> columns
) {
    public record FilterResponse(
            Long assigneeUserId,
            TaskPriority priority,
            Long labelId,
            String keyword
    ) {
    }

    public record ColumnResponse(
            TaskStatus status,
            List<TaskCardResponse> tasks
    ) {
    }

    public record TaskCardResponse(
            Long taskId,
            String title,
            TaskPriority priority,
            LocalDate dueDate,
            Integer position,
            Long version,
            AssigneeResponse assignee,
            List<LabelResponse> labels,
            Long commentCount
    ) {
    }

    public record AssigneeResponse(
            Long userId,
            String nickname
    ) {
    }

    public record LabelResponse(
            Long labelId,
            String name,
            String colorHex
    ) {
    }
}
