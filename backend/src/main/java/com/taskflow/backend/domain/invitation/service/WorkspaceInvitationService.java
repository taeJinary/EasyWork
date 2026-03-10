package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateWorkspaceInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListItemResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceSentInvitationListItemResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.entity.WorkspaceInvitation;
import com.taskflow.backend.domain.invitation.repository.WorkspaceInvitationRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceInvitationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;

    @Value("${app.invitation.expires-days:7}")
    private long invitationExpiresDays;

    @Transactional
    public WorkspaceInvitationSummaryResponse createInvitation(
            Long inviterUserId,
            Long workspaceId,
            CreateWorkspaceInvitationRequest request
    ) {
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember inviterMembership = findWorkspaceMembership(workspaceId, inviterUserId);
        ensureOwner(inviterMembership);

        User invitee = userRepository.findByEmail(request.email())
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITEE_NOT_FOUND));

        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, invitee.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        WorkspaceInvitation pendingInvitation = workspaceInvitationRepository.findByWorkspaceIdAndInviteeIdAndStatus(
                workspaceId,
                invitee.getId(),
                InvitationStatus.PENDING
        ).orElse(null);

        if (pendingInvitation != null && !normalizeExpiredPendingInvitation(pendingInvitation, now)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        WorkspaceInvitation invitation = WorkspaceInvitation.create(
                workspace,
                inviterMembership.getUser(),
                invitee,
                request.role(),
                InvitationStatus.PENDING,
                now.plusDays(invitationExpiresDays)
        );

        WorkspaceInvitation saved;
        try {
            saved = workspaceInvitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        workspace.touch(now);

        return new WorkspaceInvitationSummaryResponse(
                saved.getId(),
                workspace.getId(),
                invitee.getId(),
                invitee.getEmail(),
                invitee.getNickname(),
                saved.getRole(),
                saved.getStatus(),
                saved.getExpiresAt()
        );
    }

    @Transactional
    public WorkspaceInvitationListResponse getMyInvitations(
            Long userId,
            InvitationStatus status,
            int page,
            int size
    ) {
        findActiveUser(userId);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? size : 20;

        LocalDateTime now = LocalDateTime.now();
        List<WorkspaceInvitation> pendingInvitations =
                workspaceInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        InvitationStatus.PENDING
                );
        normalizeExpiredPendingInvitations(pendingInvitations, now);

        List<WorkspaceInvitation> invitations = status == null
                ? workspaceInvitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(userId)
                : status == InvitationStatus.PENDING
                ? pendingInvitations.stream().filter(WorkspaceInvitation::isPending).toList()
                : workspaceInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(userId, status);

        long totalElements = invitations.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        int fromIndex = Math.min(normalizedPage * normalizedSize, invitations.size());
        int toIndex = Math.min(fromIndex + normalizedSize, invitations.size());

        List<WorkspaceInvitationListItemResponse> content = invitations.subList(fromIndex, toIndex).stream()
                .map(this::toListItem)
                .toList();

        boolean first = normalizedPage == 0;
        boolean last = totalPages == 0 || normalizedPage >= totalPages - 1;
        return new WorkspaceInvitationListResponse(
                content,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                first,
                last
        );
    }

    public List<WorkspaceSentInvitationListItemResponse> getSentInvitations(
            Long userId,
            Long workspaceId,
            InvitationStatus status
    ) {
        findWorkspace(workspaceId);
        WorkspaceMember membership = findWorkspaceMembership(workspaceId, userId);
        ensureOwner(membership);

        LocalDateTime now = LocalDateTime.now();
        List<WorkspaceInvitation> pendingInvitations =
                workspaceInvitationRepository.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(
                        workspaceId,
                        InvitationStatus.PENDING
                );
        normalizeExpiredPendingInvitations(pendingInvitations, now);

        List<WorkspaceInvitation> invitations = status == null
                ? workspaceInvitationRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                : status == InvitationStatus.PENDING
                ? pendingInvitations.stream().filter(WorkspaceInvitation::isPending).toList()
                : workspaceInvitationRepository.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, status);

        return invitations.stream()
                .map(this::toSentListItem)
                .toList();
    }

    @Transactional
    public WorkspaceInvitationActionResponse acceptInvitation(Long userId, Long invitationId) {
        WorkspaceInvitation invitation = findInvitation(invitationId);
        ensureInvitee(invitation, userId);
        ensurePendingAndNotExpired(invitation);

        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(invitation.getWorkspace().getId(), userId).isPresent()) {
            throw new BusinessException(ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS);
        }

        WorkspaceMember savedMember = workspaceMemberRepository.save(
                WorkspaceMember.create(
                        invitation.getWorkspace(),
                        invitation.getInvitee(),
                        invitation.getRole(),
                        LocalDateTime.now()
                )
        );

        invitation.accept(LocalDateTime.now());
        invitation.getWorkspace().touch(LocalDateTime.now());

        return new WorkspaceInvitationActionResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                savedMember.getId(),
                invitation.getRole(),
                invitation.getStatus()
        );
    }

    @Transactional
    public WorkspaceInvitationActionResponse rejectInvitation(Long userId, Long invitationId) {
        WorkspaceInvitation invitation = findInvitation(invitationId);
        ensureInvitee(invitation, userId);
        ensurePendingAndNotExpired(invitation);

        invitation.reject(LocalDateTime.now());
        invitation.getWorkspace().touch(LocalDateTime.now());

        return new WorkspaceInvitationActionResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                null,
                invitation.getRole(),
                invitation.getStatus()
        );
    }

    @Transactional
    public WorkspaceInvitationActionResponse cancelInvitation(Long userId, Long workspaceId, Long invitationId) {
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember membership = findWorkspaceMembership(workspaceId, userId);
        ensureOwner(membership);

        WorkspaceInvitation invitation = findInvitation(invitationId);
        if (!invitation.getWorkspace().getId().equals(workspace.getId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_INVITATION_NOT_FOUND);
        }
        ensurePendingAndNotExpired(invitation);

        invitation.cancel(LocalDateTime.now());
        invitation.getWorkspace().touch(LocalDateTime.now());

        return new WorkspaceInvitationActionResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                null,
                invitation.getRole(),
                invitation.getStatus()
        );
    }

    private WorkspaceInvitationListItemResponse toListItem(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationListItemResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getInviter().getId(),
                invitation.getInviter().getNickname(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    private WorkspaceSentInvitationListItemResponse toSentListItem(WorkspaceInvitation invitation) {
        return new WorkspaceSentInvitationListItemResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getInvitee().getId(),
                invitation.getInvitee().getEmail(),
                invitation.getInvitee().getNickname(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    private WorkspaceInvitation findInvitation(Long invitationId) {
        return workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_INVITATION_NOT_FOUND));
    }

    private void ensureInvitee(WorkspaceInvitation invitation, Long userId) {
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void ensurePendingAndNotExpired(WorkspaceInvitation invitation) {
        ensurePending(invitation);
        if (normalizeExpiredPendingInvitation(invitation, LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }
    }

    private void ensurePending(WorkspaceInvitation invitation) {
        if (!invitation.isPending()) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }
    }

    private void normalizeExpiredPendingInvitations(List<WorkspaceInvitation> invitations, LocalDateTime now) {
        List<WorkspaceInvitation> expiredInvitations = invitations.stream()
                .filter(invitation -> normalizeExpiredPendingInvitation(invitation, now))
                .toList();
        if (!expiredInvitations.isEmpty()) {
            workspaceInvitationRepository.saveAll(expiredInvitations);
        }
    }

    private boolean normalizeExpiredPendingInvitation(WorkspaceInvitation invitation, LocalDateTime now) {
        if (invitation.isPending() && invitation.getExpiresAt().isBefore(now)) {
            invitation.expire(now);
            return true;
        }
        return false;
    }

    private Workspace findWorkspace(Long workspaceId) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private WorkspaceMember findWorkspaceMembership(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private void ensureOwner(WorkspaceMember membership) {
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.ONLY_OWNER_ALLOWED);
        }
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
