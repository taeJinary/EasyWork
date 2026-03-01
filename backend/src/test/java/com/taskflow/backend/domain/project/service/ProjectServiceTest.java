package com.taskflow.backend.domain.project.service;

import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectDetailResponse;
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
