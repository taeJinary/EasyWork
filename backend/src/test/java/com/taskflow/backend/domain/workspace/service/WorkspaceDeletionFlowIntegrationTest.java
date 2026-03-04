package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.comment.dto.request.CreateCommentRequest;
import com.taskflow.backend.domain.comment.service.CommentService;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
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
class WorkspaceDeletionFlowIntegrationTest extends IntegrationTestContainerSupport {

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

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void deletingWorkspaceSoftDeletesProjectsAndBlocksProjectTaskCommentAccess() {
        User owner = saveActiveUser("workspace-owner");

        WorkspaceSummaryResponse workspace = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("delete-flow-workspace", "workspace delete flow")
        );
        ProjectSummaryResponse project = projectService.createProject(
                owner.getId(),
                new CreateProjectRequest(workspace.workspaceId(), "delete-flow-project", "project delete flow")
        );
        TaskSummaryResponse task = taskService.createTask(
                owner.getId(),
                project.projectId(),
                new CreateTaskRequest(
                        "delete-flow-task",
                        "task delete flow",
                        owner.getId(),
                        TaskPriority.MEDIUM,
                        LocalDate.now().plusDays(1),
                        List.of()
                )
        );
        commentService.createComment(owner.getId(), task.taskId(), new CreateCommentRequest("workspace delete comment"));

        workspaceService.deleteWorkspace(owner.getId(), workspace.workspaceId());

        assertThat(projectRepository.findById(project.projectId())).isPresent();
        assertThat(projectRepository.findById(project.projectId()).orElseThrow().isDeleted()).isTrue();
        assertThat(projectRepository.findByIdAndDeletedAtIsNull(project.projectId())).isEmpty();
        assertThat(workspaceService.getMyWorkspaces(owner.getId(), 0, 20).content()).isEmpty();

        assertThatThrownBy(() -> workspaceService.getWorkspaceDetail(owner.getId(), workspace.workspaceId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);

        assertThatThrownBy(() -> projectService.getProjectDetail(owner.getId(), project.projectId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);

        assertThatThrownBy(() -> taskService.getTasks(
                owner.getId(),
                project.projectId(),
                0,
                20,
                null,
                "updatedAt",
                "DESC",
                null
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);

        assertThatThrownBy(() -> taskService.getTaskDetail(owner.getId(), task.taskId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);

        assertThatThrownBy(() -> commentService.getComments(owner.getId(), task.taskId(), null, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
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
