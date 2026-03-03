package com.taskflow.backend.domain.attachment.repository;

import com.taskflow.backend.domain.attachment.entity.TaskAttachmentCleanupJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAttachmentCleanupJobRepository extends JpaRepository<TaskAttachmentCleanupJob, Long> {

    boolean existsByStoragePathAndCompletedAtIsNull(String storagePath);

    long countByCompletedAtIsNull();

    List<TaskAttachmentCleanupJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime now,
            Pageable pageable
    );

    long deleteByCompletedAtIsNotNullAndUpdatedAtBefore(LocalDateTime cutoff);
}
