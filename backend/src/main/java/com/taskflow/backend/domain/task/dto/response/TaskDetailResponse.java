package com.taskflow.backend.domain.task.dto.response;

import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
        Long taskId,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Integer position,
        Long version,
        UserSummaryResponse creator,
        UserSummaryResponse assignee,
        List<LabelResponse> labels,
        Long commentCount,
        List<StatusHistoryResponse> recentStatusHistories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record UserSummaryResponse(
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

    public record StatusHistoryResponse(
            Long historyId,
            TaskStatus fromStatus,
            TaskStatus toStatus,
            UserSummaryResponse changedBy,
            LocalDateTime changedAt
    ) {
    }
}
