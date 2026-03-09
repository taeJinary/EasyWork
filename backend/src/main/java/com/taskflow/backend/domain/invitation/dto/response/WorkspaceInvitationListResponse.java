package com.taskflow.backend.domain.invitation.dto.response;

import java.util.List;

public record WorkspaceInvitationListResponse(
        List<WorkspaceInvitationListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
