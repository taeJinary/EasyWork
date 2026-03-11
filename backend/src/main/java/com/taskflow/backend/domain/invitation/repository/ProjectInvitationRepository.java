package com.taskflow.backend.domain.invitation.repository;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    Optional<ProjectInvitation> findByProjectIdAndInviteeIdAndStatus(
            Long projectId,
            Long inviteeId,
            InvitationStatus status
    );

    List<ProjectInvitation> findAllByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, InvitationStatus status);

    List<ProjectInvitation> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);

    long countByInviteeIdAndStatusAndExpiresAtAfter(Long inviteeId, InvitationStatus status, LocalDateTime expiresAt);

    long countByProjectIdAndStatusAndExpiresAtAfter(Long projectId, InvitationStatus status, LocalDateTime expiresAt);

    void deleteAllByInviteeIdOrInviterId(Long inviteeId, Long inviterId);
}

