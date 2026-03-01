package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
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
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void createInvitationCreatesPendingInvitationWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User invitee = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember ownerMember = ownerMember(100L, project, owner);
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(projectInvitationRepository.findByProjectIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.empty());
        given(projectInvitationRepository.save(any(ProjectInvitation.class))).willAnswer(invocation -> invocation.getArgument(0));

        InvitationSummaryResponse response = invitationService.createInvitation(1L, 10L, request);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.inviteeUserId()).isEqualTo(2L);
        assertThat(response.inviteeEmail()).isEqualTo("member@example.com");
        assertThat(response.role()).isEqualTo(ProjectRole.MEMBER);
        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void createInvitationThrowsWhenInviteeNotFound() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = project(10L, owner);
        ProjectMember ownerMember = ownerMember(100L, project, owner);
        CreateInvitationRequest request = new CreateInvitationRequest("none@example.com", ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.createInvitation(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVITEE_NOT_FOUND);
    }

    @Test
    void createInvitationThrowsWhenInviterIsNotOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User member = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember memberMembership = memberMember(101L, project, member);
        CreateInvitationRequest request = new CreateInvitationRequest("new@example.com", ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> invitationService.createInvitation(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);
    }

    @Test
    void createInvitationThrowsWhenInviteeAlreadyMember() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User invitee = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember ownerMembership = ownerMember(100L, project, owner);
        ProjectMember existingMember = memberMember(101L, project, invitee);
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(existingMember));

        assertThatThrownBy(() -> invitationService.createInvitation(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    void createInvitationThrowsWhenPendingInvitationAlreadyExists() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User invitee = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember ownerMembership = ownerMember(100L, project, owner);
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);
        ProjectInvitation pending = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.of(2026, 3, 8, 10, 30)
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(projectInvitationRepository.findByProjectIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.of(pending));

        assertThatThrownBy(() -> invitationService.createInvitation(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void getMyInvitationsReturnsPagedResultWithStatusFilter() {
        User invitee = activeUser(2L, "member@example.com", "팀원");
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = project(10L, owner);

        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.of(2026, 3, 8, 10, 30)
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(invitee));
        given(projectInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(2L, InvitationStatus.PENDING))
                .willReturn(List.of(invitation));

        InvitationListResponse response = invitationService.getMyInvitations(2L, InvitationStatus.PENDING, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().projectId()).isEqualTo(10L);
        assertThat(response.content().getFirst().inviterNickname()).isEqualTo("오너");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(1);
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

    private Project project(Long id, User owner) {
        return Project.builder()
                .id(id)
                .owner(owner)
                .name("TaskFlow")
                .description("프로젝트")
                .build();
    }

    private ProjectMember ownerMember(Long id, Project project, User owner) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
    }

    private ProjectMember memberMember(Long id, Project project, User member) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .user(member)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();
    }
}

