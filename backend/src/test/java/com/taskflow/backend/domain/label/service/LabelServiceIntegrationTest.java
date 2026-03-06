package com.taskflow.backend.domain.label.service;

import com.taskflow.backend.domain.label.dto.request.CreateLabelRequest;
import com.taskflow.backend.domain.label.dto.response.LabelResponse;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class LabelServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void deletingLabelRemovesTaskLabelAssociationsFromTaskDetail() {
        User owner = saveActiveUser("label-owner");
        ProjectSummaryResponse project = createProject(owner.getId(), "label-delete-project");

        LabelResponse backend = labelService.createLabel(
                owner.getId(),
                project.projectId(),
                new CreateLabelRequest("Backend", "#2563EB")
        );

        TaskSummaryResponse task = taskService.createTask(
                owner.getId(),
                project.projectId(),
                new CreateTaskRequest(
                        "label-delete-task",
                        "label integration",
                        owner.getId(),
                        TaskPriority.MEDIUM,
                        LocalDate.now().plusDays(1),
                        List.of(backend.labelId())
                )
        );

        taskService.getTaskDetail(owner.getId(), task.taskId());
        LocalDateTime updatedAtBeforeDelete = projectRepository.findById(project.projectId()).orElseThrow().getUpdatedAt();

        labelService.deleteLabel(owner.getId(), backend.labelId());

        TaskDetailResponse detail = taskService.getTaskDetail(owner.getId(), task.taskId());
        LocalDateTime updatedAtAfterDelete = projectRepository.findById(project.projectId()).orElseThrow().getUpdatedAt();

        assertThat(detail.labels()).isEmpty();
        assertThat(updatedAtAfterDelete).isAfterOrEqualTo(updatedAtBeforeDelete);
    }

    @Test
    void duplicateLabelNameWithinProjectIsRejected() {
        User owner = saveActiveUser("label-duplicate-owner");
        ProjectSummaryResponse project = createProject(owner.getId(), "label-duplicate-project");

        labelService.createLabel(
                owner.getId(),
                project.projectId(),
                new CreateLabelRequest("Backend", "#2563EB")
        );

        assertThatThrownBy(() -> labelService.createLabel(
                owner.getId(),
                project.projectId(),
                new CreateLabelRequest("Backend", "#16A34A")
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABEL_NAME_DUPLICATE);
    }

    private ProjectSummaryResponse createProject(Long ownerUserId, String projectName) {
        WorkspaceSummaryResponse workspace = workspaceService.createWorkspace(
                ownerUserId,
                new CreateWorkspaceRequest(projectName + "-workspace", "label integration workspace")
        );
        return projectService.createProject(
                ownerUserId,
                new CreateProjectRequest(workspace.workspaceId(), projectName, "label integration project")
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
