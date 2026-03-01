package com.taskflow.backend.domain.project.dto.response;

import java.util.List;

public record ProjectListResponse(
        List<ProjectListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

