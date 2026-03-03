package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.entity.TaskAttachmentCleanupJob;
import com.taskflow.backend.domain.attachment.repository.TaskAttachmentCleanupJobRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAttachmentCleanupRetryService {

    private static final int DEFAULT_BATCH_SIZE = 50;

    private final TaskAttachmentCleanupJobRepository cleanupJobRepository;
    private final TaskAttachmentStorage taskAttachmentStorage;

    @Value("${app.attachment.cleanup.retry-delay-seconds:300}")
    private long retryDelaySeconds;

    @Transactional
    public void enqueueDeleteFailure(Long attachmentId, String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }

        if (cleanupJobRepository.existsByStoragePathAndCompletedAtIsNull(storagePath)) {
            return;
        }

        TaskAttachmentCleanupJob job = TaskAttachmentCleanupJob.createPending(
                attachmentId,
                storagePath,
                LocalDateTime.now()
        );
        cleanupJobRepository.save(job);
    }

    @Transactional
    public void retryPendingDeletes(int batchSize) {
        int normalizedBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        List<TaskAttachmentCleanupJob> pendingJobs =
                cleanupJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                        LocalDateTime.now(),
                        PageRequest.of(0, normalizedBatchSize)
                );

        for (TaskAttachmentCleanupJob job : pendingJobs) {
            processDeleteJob(job);
        }
    }

    private void processDeleteJob(TaskAttachmentCleanupJob job) {
        try {
            taskAttachmentStorage.delete(job.getStoragePath());
            job.markCompleted(LocalDateTime.now());
            cleanupJobRepository.save(job);
        } catch (Exception exception) {
            job.markFailed(
                    exception.getMessage(),
                    LocalDateTime.now().plusSeconds(retryDelaySeconds)
            );
            cleanupJobRepository.save(job);

            log.error(
                    "Failed to retry attachment cleanup delete. attachmentId={}, storagePath={}, retryCount={}",
                    job.getAttachmentId(),
                    job.getStoragePath(),
                    job.getRetryCount(),
                    exception
            );
        }
    }
}
