package com.taskflow.backend.domain.task.dto.response;

import com.taskflow.backend.global.common.enums.TaskStatus;
import java.time.LocalDateTime;

public record TaskMoveResponse(
        Long taskId,
        TaskStatus status,
        Integer position,
        Long version,
        LocalDateTime completedAt
) {
}
