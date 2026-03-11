package com.taskflow.backend.domain.invitation.repository;

import com.taskflow.backend.domain.invitation.entity.WorkspaceInvitation;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    Optional<WorkspaceInvitation> findByWorkspaceIdAndInviteeIdAndStatus(
            Long workspaceId,
            Long inviteeId,
            InvitationStatus status
    );

    List<WorkspaceInvitation> findAllByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, InvitationStatus status);

    List<WorkspaceInvitation> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);

    List<WorkspaceInvitation> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(Long workspaceId, InvitationStatus status);

    List<WorkspaceInvitation> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    void deleteAllByInviteeIdOrInviterId(Long inviteeId, Long inviterId);
}
