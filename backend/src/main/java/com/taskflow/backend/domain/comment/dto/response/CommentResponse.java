package com.taskflow.backend.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        AuthorResponse author,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean editable
) {
    public record AuthorResponse(
            Long userId,
            String nickname
    ) {
    }
}
