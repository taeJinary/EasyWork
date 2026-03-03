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
    @Query("""
            delete from InvitationEmailRetryJob job
            where job.completedAt is not null
              and job.updatedAt < :cutoff
            """)
    int deleteCompletedHistoryBefore(@Param("cutoff") LocalDateTime cutoff);
}
