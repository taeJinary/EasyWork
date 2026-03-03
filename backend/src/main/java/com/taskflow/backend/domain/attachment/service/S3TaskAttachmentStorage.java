package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(name = "app.attachment.storage.type", havingValue = "s3")
public class S3TaskAttachmentStorage implements TaskAttachmentStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String keyPrefix;

    public S3TaskAttachmentStorage(
            S3Client s3Client,
            @Value("${app.attachment.storage.s3.bucket}") String bucket,
            @Value("${app.attachment.storage.s3.key-prefix:task-attachments}") String keyPrefix
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    @Override
    public StoredAttachment store(Long projectId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String objectKey = keyPrefix + "/" + projectId + "/" + storedFilename;
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/octet-stream";

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return new StoredAttachment(
                objectKey,
                storedFilename,
                contentType,
                file.getSize()
        );
    }

    @Override
    public void delete(String storagePath) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storagePath)
                    .build();
            s3Client.deleteObject(request);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "task-attachments";
        }
        String normalized = prefix.replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return StringUtils.hasText(normalized) ? normalized : "task-attachments";
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
