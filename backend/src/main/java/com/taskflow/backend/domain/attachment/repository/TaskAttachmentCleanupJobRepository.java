package com.taskflow.backend.domain.attachment.repository;

import com.taskflow.backend.domain.attachment.entity.TaskAttachmentCleanupJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAttachmentCleanupJobRepository extends JpaRepository<TaskAttachmentCleanupJob, Long> {

    boolean existsByStoragePathAndCompletedAtIsNull(String storagePath);

    long countByCompletedAtIsNull();

    List<TaskAttachmentCleanupJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime now,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from TaskAttachmentCleanupJob job
            where job.completedAt is not null
              and job.updatedAt < :cutoff
            """)
    int deleteCompletedHistoryBefore(@Param("cutoff") LocalDateTime cutoff);
}
