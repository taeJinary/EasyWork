package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(name = "app.attachment.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalTaskAttachmentStorage implements TaskAttachmentStorage {

    private final Path rootPath;

    public LocalTaskAttachmentStorage(
            @Value("${app.attachment.storage.root-path:./storage/task-attachments}") String rootPath
    ) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    @Override
    public StoredAttachment store(Long projectId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String relativePath = projectId + "/" + storedFilename;
        Path targetPath = resolveSafePath(relativePath);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/octet-stream";

        return new StoredAttachment(
                relativePath.replace("\\", "/"),
                storedFilename,
                contentType,
                file.getSize()
        );
    }

    @Override
    public void delete(String storagePath) {
        Path targetPath = resolveSafePath(storagePath);
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private Path resolveSafePath(String storagePath) {
        Path resolved = rootPath.resolve(storagePath).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return resolved;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
