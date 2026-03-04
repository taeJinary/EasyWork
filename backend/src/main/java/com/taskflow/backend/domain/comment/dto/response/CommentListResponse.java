package com.taskflow.backend.domain.comment.dto.response;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> content,
        Long nextCursor,
        boolean hasNext
) {
}
