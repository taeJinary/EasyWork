package com.taskflow.backend.domain.invitation.repository;

import com.taskflow.backend.domain.invitation.entity.InvitationEmailRetryJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationEmailRetryJobRepository extends JpaRepository<InvitationEmailRetryJob, Long> {

    boolean existsByInvitationIdAndCompletedAtIsNull(Long invitationId);

    long countByCompletedAtIsNull();

    List<InvitationEmailRetryJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime nextRetryAt,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            delete from invitation_email_retry_jobs
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
