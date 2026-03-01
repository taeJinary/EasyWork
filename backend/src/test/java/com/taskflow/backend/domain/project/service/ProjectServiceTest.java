package com.taskflow.backend.domain.project.service;

import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.request.ChangeMemberRoleRequest;
import com.taskflow.backend.domain.project.dto.request.UpdateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectDetailResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectMemberResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProjectCreatesOwnerMembership() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        CreateProjectRequest request = new CreateProjectRequest("TaskFlow", "협업용 태스크 관리 프로젝트");
        Project savedProject = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("협업용 태스크 관리 프로젝트")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(projectRepository.save(any(Project.class))).willReturn(savedProject);

        ProjectSummaryResponse response = projectService.createProject(1L, request);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("TaskFlow");
        assertThat(response.role()).isEqualTo(ProjectRole.OWNER);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void getMyProjectsReturnsPagedContent() {
        User user = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(user)
                .name("TaskFlow")
                .description("협업용 태스크 관리 프로젝트")
                .build();
        ProjectMember myMembership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(user)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectMemberRepository.findAllActiveByUserIdOrderByProjectUpdatedAtDesc(1L))
                .willReturn(List.of(myMembership));
        given(projectMemberRepository.countByProjectId(10L)).willReturn(1L);

        ProjectListResponse response = projectService.getMyProjects(1L, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().projectId()).isEqualTo(10L);
        assertThat(response.content().getFirst().role()).isEqualTo(ProjectRole.OWNER);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void getProjectDetailReturnsMembersAndSummary() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User memberUser = activeUser(2L, "member@example.com", "팀원");

        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("협업용 태스크 관리 프로젝트")
                .build();

        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember member = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(ownerMember, member));

        ProjectDetailResponse response = projectService.getProjectDetail(1L, 10L);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.myRole()).isEqualTo(ProjectRole.OWNER);
        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.pendingInvitationCount()).isEqualTo(0);
        assertThat(response.taskSummary().todo()).isEqualTo(0);
        assertThat(response.members()).hasSize(2);
    }

    @Test
    void getProjectDetailThrowsWhenNotProjectMember() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("협업용 태스크 관리 프로젝트")
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectDetail(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);
    }

    @Test
    void updateProjectUpdatesNameAndDescriptionWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("초기 설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        UpdateProjectRequest request = new UpdateProjectRequest("TaskFlow V2", "설명을 수정했습니다.");

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));

        ProjectSummaryResponse response = projectService.updateProject(1L, 10L, request);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("TaskFlow V2");
        assertThat(response.description()).isEqualTo("설명을 수정했습니다.");
        assertThat(project.getName()).isEqualTo("TaskFlow V2");
    }

    @Test
    void updateProjectThrowsWhenNotOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User memberUser = activeUser(2L, "member@example.com", "팀원");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("초기 설명")
                .build();
        ProjectMember member = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();
        UpdateProjectRequest request = new UpdateProjectRequest("TaskFlow V2", "설명을 수정했습니다.");

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> projectService.updateProject(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);
    }

    @Test
    void deleteProjectSoftDeletesWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));

        projectService.deleteProject(1L, 10L);

        assertThat(project.isDeleted()).isTrue();
    }

    @Test
    void deleteProjectThrowsWhenNotOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User memberUser = activeUser(2L, "member@example.com", "팀원");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember member = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> projectService.deleteProject(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);

        assertThat(project.isDeleted()).isFalse();
    }

    @Test
    void getProjectMembersReturnsMemberListWhenProjectMember() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User memberUser = activeUser(2L, "member@example.com", "팀원");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember member = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(10L)).willReturn(List.of(ownerMember, member));

        List<ProjectMemberResponse> response = projectService.getProjectMembers(1L, 10L);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().role()).isEqualTo(ProjectRole.OWNER);
        assertThat(response.get(1).role()).isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    void getProjectMembersThrowsWhenNotProjectMember() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectMembers(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);

        verify(projectMemberRepository, never()).findAllByProjectIdOrderByJoinedAtAsc(10L);
    }

    @Test
    void changeMemberRoleUpdatesRoleWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User memberUser = activeUser(2L, "member@example.com", "팀원");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember targetMember = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findByIdAndProjectId(101L, 10L)).willReturn(Optional.of(targetMember));

        ProjectMemberResponse response = projectService.changeMemberRole(
                1L,
                10L,
                101L,
                new ChangeMemberRoleRequest(ProjectRole.OWNER)
        );

        assertThat(response.memberId()).isEqualTo(101L);
        assertThat(response.role()).isEqualTo(ProjectRole.OWNER);
        assertThat(targetMember.getRole()).isEqualTo(ProjectRole.OWNER);
    }

    @Test
    void changeMemberRoleThrowsWhenTargetMemberNotFound() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findByIdAndProjectId(999L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.changeMemberRole(
                1L,
                10L,
                999L,
                new ChangeMemberRoleRequest(ProjectRole.MEMBER)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void changeMemberRoleThrowsWhenDemotingLastOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findByIdAndProjectId(100L, 10L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.countByProjectIdAndRole(10L, ProjectRole.OWNER)).willReturn(1L);

        assertThatThrownBy(() -> projectService.changeMemberRole(
                1L,
                10L,
                100L,
                new ChangeMemberRoleRequest(ProjectRole.MEMBER)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_REMOVE_LAST_OWNER);
    }

    @Test
    void removeMemberDeletesMemberWhenOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        User memberUser = activeUser(2L, "member@example.com", "팀원");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember targetMember = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findByIdAndProjectId(101L, 10L)).willReturn(Optional.of(targetMember));

        projectService.removeMember(1L, 10L, 101L);

        verify(projectMemberRepository).delete(targetMember);
    }

    @Test
    void removeMemberThrowsWhenRemovingLastOwner() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember ownerMember = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.findByIdAndProjectId(100L, 10L)).willReturn(Optional.of(ownerMember));
        given(projectMemberRepository.countByProjectIdAndRole(10L, ProjectRole.OWNER)).willReturn(1L);

        assertThatThrownBy(() -> projectService.removeMember(1L, 10L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_REMOVE_LAST_OWNER);

        verify(projectMemberRepository, never()).delete(any(ProjectMember.class));
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
}
