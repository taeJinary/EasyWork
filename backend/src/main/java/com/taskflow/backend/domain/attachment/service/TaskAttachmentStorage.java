package com.taskflow.backend.domain.attachment.service;

import org.springframework.web.multipart.MultipartFile;

public interface TaskAttachmentStorage {

    StoredAttachment store(Long projectId, MultipartFile file);

    void delete(String storagePath);

    record StoredAttachment(
            String storagePath,
            String storedFilename,
            String contentType,
            long sizeBytes
    ) {
    }
}
