package com.taskflow.backend.domain.invitation.dto.response;

import java.util.List;

public record InvitationListResponse(
        List<InvitationListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

