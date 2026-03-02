package com.taskflow.backend.domain.dashboard.service;

import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectStatsResponse;
import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectsResponse;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardProjectsReturnsPendingInvitationAndProjectSummaries() {
        User me = activeUser(1L, "me@example.com", "me");
        Project project = project(10L, me);
        ProjectMember membership = projectMember(100L, project, me, ProjectRole.OWNER);

        given(userRepository.findById(1L)).willReturn(Optional.of(me));
        given(projectInvitationRepository.countByInviteeIdAndStatusAndExpiresAtAfter(eq(1L), eq(InvitationStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(2L);
        given(projectMemberRepository.findAllActiveByUserIdOrderByProjectUpdatedAtDesc(1L))
                .willReturn(List.of(membership));
        given(projectMemberRepository.countByProjectId(10L)).willReturn(3L);
        given(taskRepository.countByProjectIdAndDeletedAtIsNull(10L)).willReturn(12L);
        given(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(10L, TaskStatus.DONE)).willReturn(4L);

        DashboardProjectsResponse response = dashboardService.getDashboardProjects(1L);

        assertThat(response.pendingInvitationCount()).isEqualTo(2L);
        assertThat(response.myProjects()).hasSize(1);
        assertThat(response.myProjects().getFirst().projectId()).isEqualTo(10L);
        assertThat(response.myProjects().getFirst().role()).isEqualTo(ProjectRole.OWNER);
        assertThat(response.myProjects().getFirst().progressRate()).isEqualTo(33);
    }

    @Test
    void getProjectDashboardReturnsStats() {
        User me = activeUser(1L, "me@example.com", "me");
        Project project = project(10L, me);
        ProjectMember membership = projectMember(100L, project, me, ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(projectMemberRepository.countByProjectId(10L)).willReturn(5L);
        given(taskRepository.countByProjectIdAndDeletedAtIsNull(10L)).willReturn(20L);
        given(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(10L, TaskStatus.TODO)).willReturn(8L);
        given(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(10L, TaskStatus.IN_PROGRESS)).willReturn(7L);
        given(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(10L, TaskStatus.DONE)).willReturn(5L);
        given(taskRepository.countByProjectIdAndDeletedAtIsNullAndDueDateBeforeAndStatusNot(eq(10L), any(LocalDate.class), eq(TaskStatus.DONE)))
                .willReturn(2L);
        given(taskRepository.countByProjectIdAndDeletedAtIsNullAndDueDateBetweenAndStatusNot(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(TaskStatus.DONE)))
                .willReturn(3L);

        DashboardProjectStatsResponse response = dashboardService.getProjectDashboard(1L, 10L);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.memberCount()).isEqualTo(5L);
        assertThat(response.taskCount()).isEqualTo(20L);
        assertThat(response.todoCount()).isEqualTo(8L);
        assertThat(response.inProgressCount()).isEqualTo(7L);
        assertThat(response.doneCount()).isEqualTo(5L);
        assertThat(response.overdueCount()).isEqualTo(2L);
        assertThat(response.dueSoonCount()).isEqualTo(3L);
        assertThat(response.completionRate()).isEqualTo(25);
    }

    @Test
    void getProjectDashboardThrowsWhenNotProjectMember() {
        User me = activeUser(1L, "me@example.com", "me");
        Project project = project(10L, me);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getProjectDashboard(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);
    }

    @Test
    void getDashboardProjectsThrowsWhenUserDeleted() {
        User deleted = User.builder()
                .id(1L)
                .email("deleted@example.com")
                .password("encoded")
                .nickname("deleted")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.DELETED)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> dashboardService.getDashboardProjects(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
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
                .description("desc")
                .build();
    }

    private ProjectMember projectMember(Long id, Project project, User user, ProjectRole role) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
    }
}
