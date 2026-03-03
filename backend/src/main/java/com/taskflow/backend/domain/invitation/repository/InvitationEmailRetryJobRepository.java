package com.taskflow.backend.domain.invitation.repository;

import com.taskflow.backend.domain.invitation.entity.InvitationEmailRetryJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationEmailRetryJobRepository extends JpaRepository<InvitationEmailRetryJob, Long> {

    boolean existsByInvitationIdAndCompletedAtIsNull(Long invitationId);

    long countByCompletedAtIsNull();

    List<InvitationEmailRetryJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime nextRetryAt,
            Pageable pageable
    );

    long deleteByCompletedAtIsNotNullAndUpdatedAtBefore(LocalDateTime cutoff);
}
