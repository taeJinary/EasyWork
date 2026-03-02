package com.taskflow.backend.domain.attachment.dto.response;

import java.time.LocalDateTime;

public record TaskAttachmentResponse(
        Long attachmentId,
        Long taskId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Long uploaderUserId,
        String uploaderNickname,
        LocalDateTime createdAt
) {
}
