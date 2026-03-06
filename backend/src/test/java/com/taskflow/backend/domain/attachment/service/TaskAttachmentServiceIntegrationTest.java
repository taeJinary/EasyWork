package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.dto.response.TaskAttachmentResponse;
import com.taskflow.backend.domain.attachment.entity.TaskAttachment;
import com.taskflow.backend.domain.attachment.event.TaskAttachmentDeletedEvent;
import com.taskflow.backend.domain.attachment.repository.TaskAttachmentRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RecordApplicationEvents
class TaskAttachmentServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private TaskAttachmentService taskAttachmentService;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockBean
    private TaskAttachmentStorage taskAttachmentStorage;

    @MockBean
    private OperationalMetricsService operationalMetricsService;

    @BeforeEach
    void setUp() {
        given(taskAttachmentStorage.store(any(), any())).willAnswer(invocation -> {
            Long projectId = invocation.getArgument(0);
            MockMultipartFile file = invocation.getArgument(1);
            return new TaskAttachmentStorage.StoredAttachment(
                    "task-attachments/" + projectId + "/" + file.getOriginalFilename(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );
        });
    }

    @Test
    void uploadAttachmentPersistsMetadataAndReturnsTaskAttachments() {
        User owner = saveActiveUser("attach-owner");
        Project project = saveProject(owner, "attach-project");
        saveMembership(project, owner, ProjectRole.OWNER);
        Task task = saveTask(project, owner, owner, "attach-task");
        project.touch(LocalDateTime.now().minusSeconds(5));
        projectRepository.saveAndFlush(project);
        LocalDateTime updatedAtBeforeUpload = projectRepository.findById(project.getId()).orElseThrow().getUpdatedAt();

        MockMultipartFile file = pdfFile("report.pdf");

        TaskAttachmentResponse uploaded = taskAttachmentService.uploadAttachment(owner.getId(), task.getId(), file);
        List<TaskAttachmentResponse> attachments = taskAttachmentService.getTaskAttachments(owner.getId(), task.getId());

        assertThat(uploaded.taskId()).isEqualTo(task.getId());
        assertThat(uploaded.originalFilename()).isEqualTo("report.pdf");
        assertThat(uploaded.contentType()).isEqualTo("application/pdf");
        assertThat(uploaded.sizeBytes()).isEqualTo((long) file.getSize());
        assertThat(attachments).hasSize(1);
        assertThat(attachments.getFirst().attachmentId()).isEqualTo(uploaded.attachmentId());
        assertThat(taskAttachmentRepository.findAllByTaskIdOrderByCreatedAtDesc(task.getId())).hasSize(1);
        assertThat(projectRepository.findById(project.getId()).orElseThrow().getUpdatedAt())
                .isAfter(updatedAtBeforeUpload);
    }

    @Test
    void deleteAttachmentAllowsUploaderAndPublishesDeletionEvent() {
        User owner = saveActiveUser("attach-owner");
        User uploader = saveActiveUser("attach-user");
        Project project = saveProject(owner, "attach-delete");
        saveMembership(project, owner, ProjectRole.OWNER);
        saveMembership(project, uploader, ProjectRole.MEMBER);
        Task task = saveTask(project, owner, uploader, "attach-task");

        TaskAttachmentResponse uploaded = taskAttachmentService.uploadAttachment(uploader.getId(), task.getId(), pdfFile("delete.pdf"));

        taskAttachmentService.deleteAttachment(uploader.getId(), uploaded.attachmentId());

        assertThat(taskAttachmentRepository.findById(uploaded.attachmentId())).isEmpty();
        TaskAttachmentDeletedEvent event = applicationEvents.stream(TaskAttachmentDeletedEvent.class)
                .findFirst()
                .orElseThrow();
        assertThat(event.attachmentId()).isEqualTo(uploaded.attachmentId());
        assertThat(event.storagePath()).isEqualTo("task-attachments/" + project.getId() + "/delete.pdf");
    }

    @Test
    void deleteAttachmentRejectsMemberWhoIsNeitherUploaderNorOwner() {
        User owner = saveActiveUser("attach-owner");
        User uploader = saveActiveUser("attach-up");
        User member = saveActiveUser("attach-member");
        Project project = saveProject(owner, "attach-forbid");
        saveMembership(project, owner, ProjectRole.OWNER);
        saveMembership(project, uploader, ProjectRole.MEMBER);
        saveMembership(project, member, ProjectRole.MEMBER);
        Task task = saveTask(project, owner, uploader, "attach-task");

        TaskAttachmentResponse uploaded = taskAttachmentService.uploadAttachment(uploader.getId(), task.getId(), pdfFile("forbid.pdf"));

        assertThatThrownBy(() -> taskAttachmentService.deleteAttachment(member.getId(), uploaded.attachmentId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_PERMISSION);

        assertThat(taskAttachmentRepository.findById(uploaded.attachmentId())).isPresent();
        assertThat(applicationEvents.stream(TaskAttachmentDeletedEvent.class)).isEmpty();
    }

    private Task saveTask(Project project, User creator, User assignee, String title) {
        return taskRepository.save(Task.create(
                project,
                creator,
                assignee,
                title,
                "attachment integration task",
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(1),
                0
        ));
    }

    private Project saveProject(User owner, String projectName) {
        Workspace workspace = workspaceRepository.save(Workspace.create(
                owner,
                projectName + "-workspace",
                "attachment integration workspace"
        ));
        return projectRepository.save(Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name(projectName)
                .description("attachment integration project")
                .build());
    }

    private ProjectMember saveMembership(Project project, User user, ProjectRole role) {
        return projectMemberRepository.save(ProjectMember.create(project, user, role, LocalDateTime.now()));
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

    private MockMultipartFile pdfFile(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}
        );
    }
}
