package com.taskflow.backend.domain.user.repository;

import com.taskflow.backend.domain.user.entity.EmailVerificationRetryJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRetryJobRepository extends JpaRepository<EmailVerificationRetryJob, Long> {

    boolean existsByUserIdAndCompletedAtIsNull(Long userId);

    void deleteAllByUserId(Long userId);

    long countByCompletedAtIsNull();

    List<EmailVerificationRetryJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime nextRetryAt,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            delete from email_verification_retry_jobs
            where completed_at is not null
              and updated_at < :cutoff
            order by id
            limit :deleteBatchSize
            """, nativeQuery = true)
    int deleteCompletedHistoryBefore(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("deleteBatchSize") int deleteBatchSize
    );
}
