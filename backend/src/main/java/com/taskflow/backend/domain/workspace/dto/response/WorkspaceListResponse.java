package com.taskflow.backend.domain.workspace.dto.response;

import java.util.List;

public record WorkspaceListResponse(
        List<WorkspaceListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
