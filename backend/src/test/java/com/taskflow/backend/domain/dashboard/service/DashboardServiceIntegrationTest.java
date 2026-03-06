package com.taskflow.backend.domain.dashboard.service;

import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectStatsResponse;
import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectsResponse;
import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.service.InvitationService;
import com.taskflow.backend.domain.label.dto.request.CreateLabelRequest;
import com.taskflow.backend.domain.label.service.LabelService;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.request.MoveTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskMoveResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private LabelService labelService;

    @Test
    void dashboardProjectsReflectPendingInvitationsAndUpdatedProjectOrder() {
        User owner = saveActiveUser("dashboard-owner");
        User invitee = saveActiveUser("dashboard-invitee");

        ProjectSummaryResponse firstProject = createProject(owner.getId(), "first-project");
        ProjectSummaryResponse secondProject = createProject(owner.getId(), "second-project");

        taskService.createTask(
                owner.getId(),
                firstProject.projectId(),
                new CreateTaskRequest(
                        "first-task",
                        "dashboard ordering",
                        owner.getId(),
                        TaskPriority.MEDIUM,
                        LocalDate.now().plusDays(1),
                        List.of()
                )
        );
        labelService.createLabel(
                owner.getId(),
                firstProject.projectId(),
                new CreateLabelRequest("Backend", "#2563EB")
        );

        invitationService.createInvitation(
                owner.getId(),
                secondProject.projectId(),
                new CreateInvitationRequest(invitee.getEmail(), ProjectRole.MEMBER)
        );

        DashboardProjectsResponse ownerDashboard = dashboardService.getDashboardProjects(owner.getId());
        DashboardProjectsResponse inviteeDashboard = dashboardService.getDashboardProjects(invitee.getId());

        assertThat(ownerDashboard.pendingInvitationCount()).isZero();
        assertThat(ownerDashboard.myProjects()).hasSize(2);
        assertThat(ownerDashboard.myProjects().get(0).projectId()).isEqualTo(secondProject.projectId());
        assertThat(ownerDashboard.myProjects().get(0).taskCount()).isEqualTo(0L);
        assertThat(ownerDashboard.myProjects().get(0).doneTaskCount()).isEqualTo(0L);
        assertThat(ownerDashboard.myProjects().get(0).progressRate()).isEqualTo(0);
        assertThat(ownerDashboard.myProjects().get(1).projectId()).isEqualTo(firstProject.projectId());
        assertThat(ownerDashboard.myProjects().get(1).taskCount()).isEqualTo(1L);
        assertThat(ownerDashboard.myProjects().get(1).doneTaskCount()).isEqualTo(0L);
        assertThat(ownerDashboard.myProjects().get(1).progressRate()).isEqualTo(0);

        assertThat(inviteeDashboard.pendingInvitationCount()).isEqualTo(1L);
        assertThat(inviteeDashboard.myProjects()).isEmpty();
    }

    @Test
    void projectDashboardCountsTaskStatesAndDeadlines() {
        User owner = saveActiveUser("dashboard-stats-owner");
        ProjectSummaryResponse project = createProject(owner.getId(), "stats-project");

        taskService.createTask(
                owner.getId(),
                project.projectId(),
                new CreateTaskRequest(
                        "overdue-task",
                        "dashboard overdue",
                        owner.getId(),
                        TaskPriority.HIGH,
                        LocalDate.now().minusDays(1),
                        List.of()
                )
        );

        TaskSummaryResponse inProgressTask = taskService.createTask(
                owner.getId(),
                project.projectId(),
                new CreateTaskRequest(
                        "soon-task",
                        "dashboard due soon",
                        owner.getId(),
                        TaskPriority.MEDIUM,
                        LocalDate.now().plusDays(2),
                        List.of()
                )
        );
        moveTask(owner.getId(), inProgressTask.taskId(), TaskStatus.IN_PROGRESS, 0, inProgressTask.version());

        TaskSummaryResponse doneTask = taskService.createTask(
                owner.getId(),
                project.projectId(),
                new CreateTaskRequest(
                        "done-task",
                        "dashboard done",
                        owner.getId(),
                        TaskPriority.LOW,
                        LocalDate.now().minusDays(2),
                        List.of()
                )
        );
        TaskMoveResponse doneTaskMove = moveTask(owner.getId(), doneTask.taskId(), TaskStatus.IN_PROGRESS, 1, doneTask.version());
        moveTask(owner.getId(), doneTask.taskId(), TaskStatus.DONE, 0, doneTaskMove.version());

        DashboardProjectStatsResponse stats = dashboardService.getProjectDashboard(owner.getId(), project.projectId());

        assertThat(stats.projectId()).isEqualTo(project.projectId());
        assertThat(stats.memberCount()).isEqualTo(1L);
        assertThat(stats.taskCount()).isEqualTo(3L);
        assertThat(stats.todoCount()).isEqualTo(1L);
        assertThat(stats.inProgressCount()).isEqualTo(1L);
        assertThat(stats.doneCount()).isEqualTo(1L);
        assertThat(stats.overdueCount()).isEqualTo(1L);
        assertThat(stats.dueSoonCount()).isEqualTo(1L);
        assertThat(stats.completionRate()).isEqualTo(33);
    }

    private TaskMoveResponse moveTask(Long userId, Long taskId, TaskStatus toStatus, int targetPosition, Long version) {
        return taskService.moveTask(userId, taskId, new MoveTaskRequest(toStatus, targetPosition, version));
    }

    private ProjectSummaryResponse createProject(Long ownerUserId, String projectName) {
        WorkspaceSummaryResponse workspace = workspaceService.createWorkspace(
                ownerUserId,
                new CreateWorkspaceRequest(projectName + "-workspace", "dashboard integration workspace")
        );
        return projectService.createProject(
                ownerUserId,
                new CreateProjectRequest(workspace.workspaceId(), projectName, "dashboard integration project")
        );
    }

    private User saveActiveUser(String nicknamePrefix) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname(normalizeNickname(nicknamePrefix))
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String normalizeNickname(String nicknamePrefix) {
        return nicknamePrefix.length() <= 20
                ? nicknamePrefix
                : nicknamePrefix.substring(0, 20);
    }
}
