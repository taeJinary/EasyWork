package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateWorkspaceInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListResponse;
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
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceInvitationService workspaceInvitationService;

    @Test
    void createInvitationCreatesPendingWorkspaceInvitationWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "member");
        Workspace workspace = workspace(10L, owner);
        WorkspaceMember ownerMembership = ownerMembership(100L, workspace, owner);
        CreateWorkspaceInvitationRequest request =
                new CreateWorkspaceInvitationRequest("member@example.com", WorkspaceRole.MEMBER);
        LocalDateTime beforeUpdatedAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(workspace, "updatedAt", beforeUpdatedAt);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(workspaceInvitationRepository.findByWorkspaceIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.empty());
        given(workspaceInvitationRepository.saveAndFlush(any(WorkspaceInvitation.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        WorkspaceInvitationSummaryResponse response = workspaceInvitationService.createInvitation(1L, 10L, request);

        assertThat(response.workspaceId()).isEqualTo(10L);
        assertThat(response.inviteeUserId()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(workspace.getUpdatedAt()).isAfter(beforeUpdatedAt);
    }

    @Test
    void createInvitationThrowsWhenPendingWorkspaceInvitationAlreadyExists() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "member");
        Workspace workspace = workspace(10L, owner);
        WorkspaceMember ownerMembership = ownerMembership(100L, workspace, owner);
        CreateWorkspaceInvitationRequest request =
                new CreateWorkspaceInvitationRequest("member@example.com", WorkspaceRole.MEMBER);
        WorkspaceInvitation pending = WorkspaceInvitation.create(
                workspace,
                owner,
                invitee,
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(1)
        );

        given(workspaceRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(workspaceInvitationRepository.findByWorkspaceIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.of(pending));

        assertThatThrownBy(() -> workspaceInvitationService.createInvitation(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void getMyInvitationsReturnsPagedWorkspaceInvitations() {
        User invitee = activeUser(2L, "member@example.com", "member");
        User owner = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = workspace(10L, owner);
        WorkspaceInvitation invitation = WorkspaceInvitation.create(
                workspace,
                owner,
                invitee,
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(1)
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(invitee));
        given(workspaceInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(2L, InvitationStatus.PENDING))
                .willReturn(List.of(invitation));

        WorkspaceInvitationListResponse response =
                workspaceInvitationService.getMyInvitations(2L, InvitationStatus.PENDING, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().workspaceId()).isEqualTo(10L);
        assertThat(response.content().getFirst().workspaceName()).isEqualTo("TaskFlow Team");
    }

    @Test
    void acceptInvitationCreatesWorkspaceMemberAndMarksAccepted() {
        User invitee = activeUser(2L, "member@example.com", "member");
        User inviter = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = workspace(10L, inviter);
        LocalDateTime beforeUpdatedAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(workspace, "updatedAt", beforeUpdatedAt);
        WorkspaceInvitation invitation = WorkspaceInvitation.create(
                workspace,
                inviter,
                invitee,
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(workspaceInvitationRepository.findById(30L)).willReturn(Optional.of(invitation));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(workspaceMemberRepository.save(any(WorkspaceMember.class))).willAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            return WorkspaceMember.builder()
                    .id(500L)
                    .workspace(member.getWorkspace())
                    .user(member.getUser())
                    .role(member.getRole())
                    .joinedAt(member.getJoinedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        });

        WorkspaceInvitationActionResponse response = workspaceInvitationService.acceptInvitation(2L, 30L);

        assertThat(response.workspaceId()).isEqualTo(10L);
        assertThat(response.memberId()).isEqualTo(500L);
        assertThat(response.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(workspace.getUpdatedAt()).isAfter(beforeUpdatedAt);
    }

    @Test
    void cancelInvitationMarksCanceledWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "member");
        Workspace workspace = workspace(10L, owner);
        WorkspaceMember ownerMembership = ownerMembership(100L, workspace, owner);
        WorkspaceInvitation invitation = WorkspaceInvitation.create(
                workspace,
                owner,
                invitee,
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(workspaceRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(workspaceInvitationRepository.findById(30L)).willReturn(Optional.of(invitation));

        WorkspaceInvitationActionResponse response = workspaceInvitationService.cancelInvitation(1L, 10L, 30L);

        assertThat(response.workspaceId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(InvitationStatus.CANCELED);
    }

    @Test
    void createInvitationThrowsConflictWhenConcurrentPendingInsertOccurs() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "member");
        Workspace workspace = workspace(10L, owner);
        WorkspaceMember ownerMembership = ownerMembership(100L, workspace, owner);
        CreateWorkspaceInvitationRequest request =
                new CreateWorkspaceInvitationRequest("member@example.com", WorkspaceRole.MEMBER);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(workspaceInvitationRepository.findByWorkspaceIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.empty());
        given(workspaceInvitationRepository.saveAndFlush(any(WorkspaceInvitation.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> workspaceInvitationService.createInvitation(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    private User activeUser(Long id, String email, String nickname) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .nickname(nickname)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Workspace workspace(Long id, User owner) {
        Workspace workspace = Workspace.create(owner, "TaskFlow Team", "workspace");
        ReflectionTestUtils.setField(workspace, "id", id);
        return workspace;
    }

    private WorkspaceMember ownerMembership(Long id, Workspace workspace, User owner) {
        WorkspaceMember membership = WorkspaceMember.create(workspace, owner, WorkspaceRole.OWNER, LocalDateTime.now());
        ReflectionTestUtils.setField(membership, "id", id);
        return membership;
    }
}
