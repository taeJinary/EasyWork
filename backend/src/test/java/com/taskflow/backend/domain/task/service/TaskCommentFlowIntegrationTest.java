package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.comment.dto.request.CreateCommentRequest;
import com.taskflow.backend.domain.comment.dto.response.CommentListResponse;
import com.taskflow.backend.domain.comment.dto.response.CommentResponse;
import com.taskflow.backend.domain.comment.service.CommentService;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
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
class TaskCommentFlowIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CommentService commentService;

    @Test
    void commentLifecycleKeepsTaskCommentCountsConsistentAcrossViews() {
        User owner = saveActiveUser("flow-owner");

        WorkspaceSummaryResponse workspace = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("flow-workspace", "integration-workspace")
        );
        ProjectSummaryResponse project = projectService.createProject(
                owner.getId(),
                new CreateProjectRequest(workspace.workspaceId(), "flow-project", "integration-project")
        );
        TaskSummaryResponse task = taskService.createTask(
                owner.getId(),
                project.projectId(),
                new CreateTaskRequest(
                        "flow-task",
                        "integration-task",
                        owner.getId(),
                        TaskPriority.HIGH,
                        LocalDate.now().plusDays(1),
                        List.of()
                )
        );

        CommentResponse createdComment = commentService.createComment(
                owner.getId(),
                task.taskId(),
                new CreateCommentRequest("first integration comment")
        );

        assertThat(taskService.getTaskDetail(owner.getId(), task.taskId()).commentCount()).isEqualTo(1L);
        assertThat(taskService.getTasks(
                owner.getId(),
                project.projectId(),
                0,
                20,
                null,
                "updatedAt",
                "DESC",
                null
        ).content().get(0).commentCount()).isEqualTo(1L);
        assertThat(findCardByTaskId(taskService.getTaskBoard(
                owner.getId(),
                project.projectId(),
                null,
                null,
                null,
                null
        ), task.taskId()).commentCount()).isEqualTo(1L);
        assertThat(commentService.getComments(owner.getId(), task.taskId(), null, 20).content())
                .extracting(CommentResponse::commentId)
                .containsExactly(createdComment.commentId());

        commentService.deleteComment(owner.getId(), createdComment.commentId());

        TaskDetailResponse detailAfterDelete = taskService.getTaskDetail(owner.getId(), task.taskId());
        TaskListResponse listAfterDelete = taskService.getTasks(
                owner.getId(),
                project.projectId(),
                0,
                20,
                null,
                "updatedAt",
                "DESC",
                null
        );
        TaskBoardResponse boardAfterDelete = taskService.getTaskBoard(
                owner.getId(),
                project.projectId(),
                null,
                null,
                null,
                null
        );
        CommentListResponse commentsAfterDelete = commentService.getComments(owner.getId(), task.taskId(), null, 20);

        assertThat(detailAfterDelete.commentCount()).isZero();
        assertThat(listAfterDelete.content().get(0).commentCount()).isZero();
        assertThat(findCardByTaskId(boardAfterDelete, task.taskId()).commentCount()).isZero();
        assertThat(commentsAfterDelete.content()).isEmpty();
    }

    private TaskBoardResponse.TaskCardResponse findCardByTaskId(TaskBoardResponse board, Long taskId) {
        return board.columns().stream()
                .filter(column -> column.status() == TaskStatus.TODO)
                .flatMap(column -> column.tasks().stream())
                .filter(card -> card.taskId().equals(taskId))
                .findFirst()
                .orElseThrow();
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
