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
import java.time.LocalDateTime;
import java.util.List;
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

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskAttachmentStorage taskAttachmentStorage;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public TaskAttachmentResponse uploadAttachment(Long userId, Long taskId, MultipartFile file) {
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
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
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
            log.warn("Failed to cleanup stored attachment file. storagePath={}", storagePath, exception);
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
