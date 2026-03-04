package com.taskflow.backend.domain.attachment.event;

public record TaskAttachmentDeletedEvent(
        Long attachmentId,
        String storagePath
) {
}
