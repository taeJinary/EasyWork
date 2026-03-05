package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.dto.response.TaskAttachmentResponse;
import com.taskflow.backend.domain.attachment.entity.TaskAttachment;
import com.taskflow.backend.domain.attachment.event.TaskAttachmentDeletedEvent;
import com.taskflow.backend.domain.attachment.repository.TaskAttachmentRepository;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.logging.SensitiveValueSanitizer;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAttachmentService {

    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip"
    );
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("png", Set.of("image/png")),
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/octet-stream"
            )),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("xlsx", Set.of(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/octet-stream"
            )),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint", "application/octet-stream")),
            Map.entry("pptx", Set.of(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/octet-stream"
            )),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    );
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };
    private static final byte[] GIF87A_SIGNATURE = new byte[]{
            0x47, 0x49, 0x46, 0x38, 0x37, 0x61
    };
    private static final byte[] GIF89A_SIGNATURE = new byte[]{
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61
    };
    private static final byte[] PDF_SIGNATURE = new byte[]{
            0x25, 0x50, 0x44, 0x46, 0x2D
    };
    private static final byte[] ZIP_LOCAL_FILE_SIGNATURE = new byte[]{
            0x50, 0x4B, 0x03, 0x04
    };
    private static final byte[] ZIP_EMPTY_ARCHIVE_SIGNATURE = new byte[]{
            0x50, 0x4B, 0x05, 0x06
    };
    private static final byte[] ZIP_SPANNED_ARCHIVE_SIGNATURE = new byte[]{
            0x50, 0x4B, 0x07, 0x08
    };
    private static final byte[] OLE_SIGNATURE = new byte[]{
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final List<byte[]> ZIP_SIGNATURES = List.of(
            ZIP_LOCAL_FILE_SIGNATURE,
            ZIP_EMPTY_ARCHIVE_SIGNATURE,
            ZIP_SPANNED_ARCHIVE_SIGNATURE
    );
    private static final Map<String, List<byte[]>> ALLOWED_SIGNATURES_BY_EXTENSION = Map.ofEntries(
            Map.entry("png", List.of(PNG_SIGNATURE)),
            Map.entry("jpg", List.of(JPEG_SIGNATURE)),
            Map.entry("jpeg", List.of(JPEG_SIGNATURE)),
            Map.entry("gif", List.of(GIF87A_SIGNATURE, GIF89A_SIGNATURE)),
            Map.entry("pdf", List.of(PDF_SIGNATURE)),
            Map.entry("doc", List.of(OLE_SIGNATURE)),
            Map.entry("xls", List.of(OLE_SIGNATURE)),
            Map.entry("ppt", List.of(OLE_SIGNATURE)),
            Map.entry("docx", ZIP_SIGNATURES),
            Map.entry("xlsx", ZIP_SIGNATURES),
            Map.entry("pptx", ZIP_SIGNATURES),
            Map.entry("zip", ZIP_SIGNATURES)
    );

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskAttachmentStorage taskAttachmentStorage;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OperationalMetricsService operationalMetricsService;

    @Transactional
    public TaskAttachmentResponse uploadAttachment(Long userId, Long taskId, MultipartFile file) {
        try {
            Task task = findActiveTask(taskId);
            ProjectMember membership = findMembership(task.getProject().getId(), userId);
            validateAttachmentFile(file);

            TaskAttachmentStorage.StoredAttachment storedAttachment =
                    taskAttachmentStorage.store(task.getProject().getId(), file);
            AtomicBoolean cleanupCompleted = new AtomicBoolean(false);

            try {
                String originalFilename = file.getOriginalFilename();
                TaskAttachment attachment = TaskAttachment.create(
                        task,
                        membership.getUser(),
                        StringUtils.hasText(originalFilename) ? originalFilename : storedAttachment.storedFilename(),
                        storedAttachment.storedFilename(),
                        storedAttachment.storagePath(),
                        storedAttachment.contentType(),
                        storedAttachment.sizeBytes()
                );

                TaskAttachment saved = taskAttachmentRepository.saveAndFlush(attachment);
                registerRollbackCleanup(storedAttachment.storagePath(), cleanupCompleted);
                task.getProject().touch(LocalDateTime.now());
                return toResponse(saved);
            } catch (RuntimeException exception) {
                cleanupStoredFileIfNeeded(storedAttachment.storagePath(), cleanupCompleted);
                throw exception;
            }
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementFileUploadFailure();
            throw exception;
        }
    }

    public List<TaskAttachmentResponse> getTaskAttachments(Long userId, Long taskId) {
        Task task = findActiveTask(taskId);
        findMembership(task.getProject().getId(), userId);
        return taskAttachmentRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteAttachment(Long userId, Long attachmentId) {
        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        Long projectId = attachment.getTask().getProject().getId();
        ProjectMember membership = findMembership(projectId, userId);
        boolean isUploader = attachment.getUploader().getId().equals(userId);
        boolean isOwner = membership.getRole() == ProjectRole.OWNER;
        if (!isUploader && !isOwner) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        taskAttachmentRepository.delete(attachment);
        attachment.getTask().getProject().touch(LocalDateTime.now());
        applicationEventPublisher.publishEvent(
                new TaskAttachmentDeletedEvent(attachment.getId(), attachment.getStoragePath())
        );
    }

    private Task findActiveTask(Long taskId) {
        return taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private void validateAttachmentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String extension = extractExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        validateMimeType(extension, file.getContentType());
        validateFileSignature(extension, file);
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void validateMimeType(String extension, String contentType) {
        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES_BY_EXTENSION.get(extension);
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String normalizedContentType = normalizeContentType(contentType);
        if (!allowedMimeTypes.contains(normalizedContentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        String trimmed = contentType.trim().toLowerCase(Locale.ROOT);
        int delimiterIndex = trimmed.indexOf(';');
        return delimiterIndex >= 0 ? trimmed.substring(0, delimiterIndex).trim() : trimmed;
    }

    private void validateFileSignature(String extension, MultipartFile file) {
        List<byte[]> allowedSignatures = ALLOWED_SIGNATURES_BY_EXTENSION.get(extension);
        if (allowedSignatures == null || allowedSignatures.isEmpty()) {
            return;
        }

        int maxSignatureLength = allowedSignatures.stream()
                .mapToInt(signature -> signature.length)
                .max()
                .orElse(0);
        byte[] filePrefix = readFilePrefix(file, maxSignatureLength);
        boolean signatureMatched = allowedSignatures.stream()
                .anyMatch(signature -> startsWith(filePrefix, signature));
        if (!signatureMatched) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private byte[] readFilePrefix(MultipartFile file, int maxLength) {
        if (maxLength <= 0) {
            return new byte[0];
        }
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(maxLength);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private TaskAttachmentResponse toResponse(TaskAttachment attachment) {
        return new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getTask().getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploader().getId(),
                attachment.getUploader().getNickname(),
                attachment.getCreatedAt()
        );
    }

    private void cleanupStoredFile(String storagePath) {
        try {
            taskAttachmentStorage.delete(storagePath);
        } catch (Exception exception) {
            log.warn(
                    "Failed to cleanup stored attachment file. storagePathHash={}",
                    SensitiveValueSanitizer.shortHash(storagePath),
                    exception
            );
        }
    }

    private void registerRollbackCleanup(String storagePath, AtomicBoolean cleanupCompleted) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    cleanupStoredFileIfNeeded(storagePath, cleanupCompleted);
                }
            }
        });
    }

    private void cleanupStoredFileIfNeeded(String storagePath, AtomicBoolean cleanupCompleted) {
        if (cleanupCompleted.compareAndSet(false, true)) {
            cleanupStoredFile(storagePath);
        }
    }
}
