package com.taskflow.backend.domain.task.dto.response;

import java.util.List;

public record TaskListResponse(
        List<TaskListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
