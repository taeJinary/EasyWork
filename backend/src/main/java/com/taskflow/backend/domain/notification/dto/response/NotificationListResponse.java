package com.taskflow.backend.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
