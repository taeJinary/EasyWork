package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationActionResponse;
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
import com.taskflow.backend.domain.notification.service.NotificationService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void createInvitationCreatesPendingInvitationWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User invitee = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember ownerMember = ownerMember(100L, project, owner);
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(projectInvitationRepository.findByProjectIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.empty());
        given(projectInvitationRepository.saveAndFlush(any(ProjectInvitation.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        InvitationSummaryResponse response = invitationService.createInvitation(1L, 10L, request);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.inviteeUserId()).isEqualTo(2L);
        assertThat(response.inviteeEmail()).isEqualTo("member@example.com");
        assertThat(response.role()).isEqualTo(ProjectRole.MEMBER);
        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
        verify(notificationService).createInvitationNotification(any(ProjectInvitation.class));
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
    void createInvitationThrowsConflictWhenConcurrentPendingInsertOccurs() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User invitee = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember ownerMembership = ownerMember(100L, project, owner);
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(projectInvitationRepository.findByProjectIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.empty());
        given(projectInvitationRepository.saveAndFlush(any(ProjectInvitation.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> invitationService.createInvitation(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void createInvitationNormalizesExpiredPendingAndCreatesNewInvitation() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "invitee");
        Project project = project(10L, owner);
        ProjectMember ownerMembership = ownerMember(100L, project, owner);
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);
        ProjectInvitation expiredPending = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().minusHours(1)
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(userRepository.findByEmail("member@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(projectInvitationRepository.findByProjectIdAndInviteeIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .willReturn(Optional.of(expiredPending));
        given(projectInvitationRepository.saveAndFlush(any(ProjectInvitation.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        InvitationSummaryResponse response = invitationService.createInvitation(1L, 10L, request);

        assertThat(expiredPending.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.inviteeUserId()).isEqualTo(2L);
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

    @Test
    void getMyInvitationsFiltersOutExpiredPendingAfterNormalization() {
        User invitee = activeUser(2L, "member@example.com", "invitee");
        User owner = activeUser(1L, "owner@example.com", "owner");
        Project project = project(10L, owner);
        ProjectInvitation expiredPending = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().minusMinutes(30)
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(invitee));
        given(projectInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(2L, InvitationStatus.PENDING))
                .willReturn(List.of(expiredPending));

        InvitationListResponse response = invitationService.getMyInvitations(2L, InvitationStatus.PENDING, 0, 20);

        assertThat(expiredPending.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void acceptInvitationCreatesMemberAndMarksAccepted() {
        User invitee = activeUser(2L, "member@example.com", "팀원");
        User inviter = activeUser(1L, "owner@example.com", "오너");
        Project project = project(10L, inviter);
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviter,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());
        given(projectMemberRepository.save(any(ProjectMember.class))).willAnswer(invocation -> {
            ProjectMember member = invocation.getArgument(0);
            return ProjectMember.builder()
                    .id(500L)
                    .project(member.getProject())
                    .user(member.getUser())
                    .role(member.getRole())
                    .joinedAt(member.getJoinedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        });

        InvitationActionResponse response = invitationService.acceptInvitation(2L, 10L);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.memberId()).isEqualTo(500L);
        assertThat(response.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
        verify(notificationService).createInvitationAcceptedNotification(invitation);
    }

    @Test
    void acceptInvitationThrowsWhenNotInvitee() {
        User invitee = activeUser(2L, "member@example.com", "팀원");
        User inviter = activeUser(1L, "owner@example.com", "오너");
        Project project = project(10L, inviter);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviter,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(3L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void rejectInvitationMarksRejected() {
        User invitee = activeUser(2L, "member@example.com", "팀원");
        User inviter = activeUser(1L, "owner@example.com", "오너");
        Project project = project(10L, inviter);
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviter,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        InvitationActionResponse response = invitationService.rejectInvitation(2L, 10L);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(InvitationStatus.REJECTED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
    }

    @Test
    void cancelInvitationMarksCanceledWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User invitee = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);
        ProjectMember ownerMembership = ownerMember(100L, project, owner);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        InvitationActionResponse response = invitationService.cancelInvitation(1L, 10L, 10L);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(InvitationStatus.CANCELED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELED);
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
    }

    @Test
    void cancelInvitationThrowsWhenNotOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User member = activeUser(2L, "member@example.com", "팀원");
        Project project = project(10L, owner);
        ProjectMember memberMembership = memberMember(101L, project, member);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                owner,
                member,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> invitationService.cancelInvitation(2L, 10L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);
    }

    @Test
    void cancelInvitationExpiresAndThrowsWhenInvitationAlreadyExpired() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "invitee");
        Project project = project(10L, owner);
        ProjectMember ownerMembership = ownerMember(100L, project, owner);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().minusDays(1)
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMembership));
        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.cancelInvitation(1L, 10L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVITATION_ALREADY_PROCESSED);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void acceptInvitationThrowsWhenAlreadyProcessed() {
        User invitee = activeUser(2L, "member@example.com", "팀원");
        User inviter = activeUser(1L, "owner@example.com", "오너");
        Project project = project(10L, inviter);
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviter,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.REJECTED,
                LocalDateTime.now().plusDays(7)
        );

        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVITATION_ALREADY_PROCESSED);

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
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
