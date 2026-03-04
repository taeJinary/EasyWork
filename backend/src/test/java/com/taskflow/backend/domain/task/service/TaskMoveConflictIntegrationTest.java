package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.request.MoveTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskMoveConflictIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    @Test
    void staleVersionMoveRequestIsRejectedAndKeepsBoardConsistent() {
        User owner = saveActiveUser("move-conflict-owner");

        WorkspaceSummaryResponse workspace = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("move-conflict-workspace", "move conflict integration")
        );
        ProjectSummaryResponse project = projectService.createProject(
                owner.getId(),
                new CreateProjectRequest(workspace.workspaceId(), "move-conflict-project", "move conflict integration")
        );

        TaskSummaryResponse firstTask = taskService.createTask(
                owner.getId(),
                project.projectId(),
                createTaskRequest("task-1", owner.getId())
        );
        TaskSummaryResponse secondTask = taskService.createTask(
                owner.getId(),
                project.projectId(),
                createTaskRequest("task-2", owner.getId())
        );
        TaskSummaryResponse thirdTask = taskService.createTask(
                owner.getId(),
                project.projectId(),
                createTaskRequest("task-3", owner.getId())
        );

        taskService.moveTask(
                owner.getId(),
                firstTask.taskId(),
                new MoveTaskRequest(TaskStatus.IN_PROGRESS, 0, firstTask.version())
        );

        assertThatThrownBy(() -> taskService.moveTask(
                owner.getId(),
                firstTask.taskId(),
                new MoveTaskRequest(TaskStatus.DONE, 0, firstTask.version())
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_CONFLICT);

        TaskDetailResponse movedTaskDetail = taskService.getTaskDetail(owner.getId(), firstTask.taskId());
        TaskBoardResponse board = taskService.getTaskBoard(
                owner.getId(),
                project.projectId(),
                null,
                null,
                null,
                null
        );

        assertThat(movedTaskDetail.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(movedTaskDetail.position()).isZero();
        assertThat(movedTaskDetail.recentStatusHistories()).hasSize(1);

        List<TaskBoardResponse.TaskCardResponse> todoCards = findColumn(board, TaskStatus.TODO).tasks();
        List<TaskBoardResponse.TaskCardResponse> inProgressCards = findColumn(board, TaskStatus.IN_PROGRESS).tasks();
        List<TaskBoardResponse.TaskCardResponse> doneCards = findColumn(board, TaskStatus.DONE).tasks();

        assertThat(todoCards)
                .extracting(TaskBoardResponse.TaskCardResponse::taskId)
                .containsExactly(secondTask.taskId(), thirdTask.taskId());
        assertThat(todoCards)
                .extracting(TaskBoardResponse.TaskCardResponse::position)
                .containsExactly(0, 1);
        assertThat(inProgressCards)
                .extracting(TaskBoardResponse.TaskCardResponse::taskId)
                .containsExactly(firstTask.taskId());
        assertThat(inProgressCards.get(0).position()).isZero();
        assertThat(doneCards).isEmpty();
    }

    private TaskBoardResponse.ColumnResponse findColumn(TaskBoardResponse board, TaskStatus status) {
        return board.columns().stream()
                .filter(column -> column.status() == status)
                .findFirst()
                .orElseThrow();
    }

    private CreateTaskRequest createTaskRequest(String title, Long assigneeUserId) {
        return new CreateTaskRequest(
                title,
                "move conflict integration task",
                assigneeUserId,
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(1),
                List.of()
        );
    }

    private User saveActiveUser(String nicknamePrefix) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname(nicknamePrefix)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
