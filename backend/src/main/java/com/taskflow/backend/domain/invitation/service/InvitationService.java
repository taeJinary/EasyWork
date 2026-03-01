package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListItemResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final UserRepository userRepository;

    @Value("${app.invitation.expires-days:7}")
    private long invitationExpiresDays;

    @Transactional
    public InvitationSummaryResponse createInvitation(
            Long inviterUserId,
            Long projectId,
            CreateInvitationRequest request
    ) {
        Project project = findActiveProject(projectId);
        ProjectMember inviterMembership = findMembership(projectId, inviterUserId);
        ensureOwner(inviterMembership);

        User invitee = userRepository.findByEmail(request.email())
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITEE_NOT_FOUND));

        if (projectMemberRepository.findByProjectIdAndUserId(projectId, invitee.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        if (projectInvitationRepository.findByProjectIdAndInviteeIdAndStatus(
                projectId,
                invitee.getId(),
                InvitationStatus.PENDING
        ).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(invitationExpiresDays);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviterMembership.getUser(),
                invitee,
                request.role(),
                InvitationStatus.PENDING,
                expiresAt
        );

        ProjectInvitation saved = projectInvitationRepository.save(invitation);
        return new InvitationSummaryResponse(
                saved.getId(),
                project.getId(),
                invitee.getId(),
                invitee.getEmail(),
                invitee.getNickname(),
                saved.getRole(),
                saved.getStatus(),
                saved.getExpiresAt()
        );
    }

    public InvitationListResponse getMyInvitations(
            Long userId,
            InvitationStatus status,
            int page,
            int size
    ) {
        findActiveUser(userId);

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? size : 20;

        List<ProjectInvitation> invitations = status == null
                ? projectInvitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(userId)
                : projectInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(userId, status);

        long totalElements = invitations.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        int fromIndex = Math.min(normalizedPage * normalizedSize, invitations.size());
        int toIndex = Math.min(fromIndex + normalizedSize, invitations.size());

        List<InvitationListItemResponse> content = invitations.subList(fromIndex, toIndex).stream()
                .map(this::toInvitationListItemResponse)
                .toList();

        boolean first = normalizedPage == 0;
        boolean last = totalPages == 0 || normalizedPage >= totalPages - 1;

        return new InvitationListResponse(
                content,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                first,
                last
        );
    }

    @Transactional
    public InvitationActionResponse acceptInvitation(Long userId, Long invitationId) {
        ProjectInvitation invitation = findInvitation(invitationId);
        ensureInvitee(invitation, userId);
        ensurePendingAndNotExpired(invitation);

        if (projectMemberRepository.findByProjectIdAndUserId(
                invitation.getProject().getId(),
                userId
        ).isPresent()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        ProjectMember savedMember = projectMemberRepository.save(
                ProjectMember.create(
                        invitation.getProject(),
                        invitation.getInvitee(),
                        invitation.getRole(),
                        LocalDateTime.now()
                )
        );

        invitation.accept(LocalDateTime.now());

        return new InvitationActionResponse(
                invitation.getId(),
                invitation.getProject().getId(),
                savedMember.getId(),
                invitation.getRole(),
                invitation.getStatus()
        );
    }

    @Transactional
    public InvitationActionResponse rejectInvitation(Long userId, Long invitationId) {
        ProjectInvitation invitation = findInvitation(invitationId);
        ensureInvitee(invitation, userId);
        ensurePendingAndNotExpired(invitation);

        invitation.reject(LocalDateTime.now());

        return new InvitationActionResponse(
                invitation.getId(),
                invitation.getProject().getId(),
                null,
                invitation.getRole(),
                invitation.getStatus()
        );
    }

    @Transactional
    public InvitationActionResponse cancelInvitation(Long userId, Long projectId, Long invitationId) {
        Project project = findActiveProject(projectId);
        ProjectMember membership = findMembership(projectId, userId);
        ensureOwner(membership);

        ProjectInvitation invitation = findInvitation(invitationId);
        if (!invitation.getProject().getId().equals(project.getId())) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }
        ensurePending(invitation);

        invitation.cancel(LocalDateTime.now());

        return new InvitationActionResponse(
                invitation.getId(),
                invitation.getProject().getId(),
                null,
                invitation.getRole(),
                invitation.getStatus()
        );
    }

    private InvitationListItemResponse toInvitationListItemResponse(ProjectInvitation invitation) {
        return new InvitationListItemResponse(
                invitation.getId(),
                invitation.getProject().getId(),
                invitation.getProject().getName(),
                invitation.getInviter().getId(),
                invitation.getInviter().getNickname(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    private ProjectInvitation findInvitation(Long invitationId) {
        return projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private void ensureInvitee(ProjectInvitation invitation, Long userId) {
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void ensurePendingAndNotExpired(ProjectInvitation invitation) {
        ensurePending(invitation);
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.expire(LocalDateTime.now());
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }
    }

    private void ensurePending(ProjectInvitation invitation) {
        if (!invitation.isPending()) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private void ensureOwner(ProjectMember membership) {
        if (membership.getRole() != ProjectRole.OWNER) {
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

