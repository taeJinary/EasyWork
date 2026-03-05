package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.dto.response.TaskAttachmentResponse;
import com.taskflow.backend.domain.attachment.entity.TaskAttachment;
import com.taskflow.backend.domain.attachment.event.TaskAttachmentDeletedEvent;
import com.taskflow.backend.domain.attachment.repository.TaskAttachmentRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private TaskAttachmentStorage taskAttachmentStorage;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @InjectMocks
    private TaskAttachmentService taskAttachmentService;

    @Test
    void uploadAttachmentStoresFileAndMetadataWhenProjectMember() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 10, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                validPdfBytes()
        );

        TaskAttachmentStorage.StoredAttachment storedAttachment = new TaskAttachmentStorage.StoredAttachment(
                "task-attachments/10/abc-report.pdf",
                "report.pdf",
                "application/pdf",
                5L
        );

        TaskAttachment saved = TaskAttachment.builder()
                .id(2000L)
                .task(task)
                .uploader(uploader)
                .originalFilename("report.pdf")
                .storedFilename("abc-report.pdf")
                .storagePath("task-attachments/10/abc-report.pdf")
                .contentType("application/pdf")
                .sizeBytes(5L)
                .build();
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 3, 3, 10, 1));

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskAttachmentStorage.store(eq(10L), any(MultipartFile.class))).willReturn(storedAttachment);
        given(taskAttachmentRepository.saveAndFlush(any(TaskAttachment.class))).willReturn(saved);

        TaskAttachmentResponse response = taskAttachmentService.uploadAttachment(1L, 100L, multipartFile);

        assertThat(response.attachmentId()).isEqualTo(2000L);
        assertThat(response.taskId()).isEqualTo(100L);
        assertThat(response.originalFilename()).isEqualTo("report.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.sizeBytes()).isEqualTo(5L);
        assertThat(response.uploaderNickname()).isEqualTo("uploader");
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
    }

    @Test
    void uploadAttachmentThrowsWhenExtensionIsNotAllowed() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "hello".getBytes()
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 100L, multipartFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(operationalMetricsService).incrementFileUploadFailure();
        verify(taskAttachmentRepository, never()).save(any(TaskAttachment.class));
        verify(taskAttachmentStorage, never()).store(any(), any(MultipartFile.class));
    }

    @Test
    void uploadAttachmentThrowsWhenMimeTypeDoesNotMatchExtension() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "report.pdf",
                "image/png",
                "hello".getBytes()
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 100L, multipartFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(taskAttachmentStorage, never()).store(any(), any(MultipartFile.class));
    }

    @Test
    void uploadAttachmentThrowsWhenFileSignatureDoesNotMatchExtensionAndMime() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 100L, multipartFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(taskAttachmentStorage, never()).store(any(), any(MultipartFile.class));
    }

    @Test
    void uploadAttachmentDeletesStoredFileWhenRepositorySaveFails() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                validPdfBytes()
        );
        TaskAttachmentStorage.StoredAttachment storedAttachment = new TaskAttachmentStorage.StoredAttachment(
                "task-attachments/10/abc-report.pdf",
                "abc-report.pdf",
                "application/pdf",
                5L
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskAttachmentStorage.store(eq(10L), any(MultipartFile.class))).willReturn(storedAttachment);
        given(taskAttachmentRepository.saveAndFlush(any(TaskAttachment.class))).willThrow(new RuntimeException("db fail"));

        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(1L, 100L, multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db fail");

        verify(operationalMetricsService).incrementFileUploadFailure();
        verify(taskAttachmentStorage).delete("task-attachments/10/abc-report.pdf");
    }

    @Test
    void uploadAttachmentCleansStoredFileWhenRollbackHappensAfterMethodReturn() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                validPdfBytes()
        );

        TaskAttachmentStorage.StoredAttachment storedAttachment = new TaskAttachmentStorage.StoredAttachment(
                "task-attachments/10/abc-report.pdf",
                "abc-report.pdf",
                "application/pdf",
                5L
        );

        TaskAttachment saved = TaskAttachment.builder()
                .id(2000L)
                .task(task)
                .uploader(uploader)
                .originalFilename("report.pdf")
                .storedFilename("abc-report.pdf")
                .storagePath("task-attachments/10/abc-report.pdf")
                .contentType("application/pdf")
                .sizeBytes(5L)
                .build();
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 3, 3, 10, 1));

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskAttachmentStorage.store(eq(10L), any(MultipartFile.class))).willReturn(storedAttachment);
        given(taskAttachmentRepository.saveAndFlush(any(TaskAttachment.class))).willReturn(saved);

        TransactionSynchronizationManager.initSynchronization();
        try {
            taskAttachmentService.uploadAttachment(1L, 100L, multipartFile);
            verify(taskAttachmentStorage, never()).delete("task-attachments/10/abc-report.pdf");

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            verify(taskAttachmentStorage).delete("task-attachments/10/abc-report.pdf");
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Test
    void getTaskAttachmentsReturnsLatestFirst() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);

        TaskAttachment newer = TaskAttachment.builder()
                .id(2L)
                .task(task)
                .uploader(uploader)
                .originalFilename("newer.pdf")
                .storedFilename("newer.pdf")
                .storagePath("task-attachments/10/newer.pdf")
                .contentType("application/pdf")
                .sizeBytes(5L)
                .build();
        ReflectionTestUtils.setField(newer, "createdAt", LocalDateTime.of(2026, 3, 3, 11, 0));

        TaskAttachment older = TaskAttachment.builder()
                .id(1L)
                .task(task)
                .uploader(uploader)
                .originalFilename("older.pdf")
                .storedFilename("older.pdf")
                .storagePath("task-attachments/10/older.pdf")
                .contentType("application/pdf")
                .sizeBytes(5L)
                .build();
        ReflectionTestUtils.setField(older, "createdAt", LocalDateTime.of(2026, 3, 3, 10, 0));

        given(taskRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskAttachmentRepository.findAllByTaskIdOrderByCreatedAtDesc(100L)).willReturn(List.of(newer, older));

        List<TaskAttachmentResponse> response = taskAttachmentService.getTaskAttachments(1L, 100L);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().attachmentId()).isEqualTo(2L);
        assertThat(response.get(1).attachmentId()).isEqualTo(1L);
    }

    @Test
    void deleteAttachmentPublishesDeleteEventWhenUploader() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, uploader, ProjectRole.MEMBER);
        TaskAttachment attachment = TaskAttachment.builder()
                .id(2000L)
                .task(task)
                .uploader(uploader)
                .originalFilename("report.pdf")
                .storedFilename("abc-report.pdf")
                .storagePath("task-attachments/10/abc-report.pdf")
                .contentType("application/pdf")
                .sizeBytes(5L)
                .build();

        given(taskAttachmentRepository.findById(2000L)).willReturn(Optional.of(attachment));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        taskAttachmentService.deleteAttachment(1L, 2000L);

        verify(taskAttachmentRepository).delete(attachment);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(TaskAttachmentDeletedEvent.class);
        TaskAttachmentDeletedEvent deletedEvent = (TaskAttachmentDeletedEvent) eventCaptor.getValue();
        assertThat(deletedEvent.attachmentId()).isEqualTo(2000L);
        assertThat(deletedEvent.storagePath()).isEqualTo("task-attachments/10/abc-report.pdf");
        verify(taskAttachmentStorage, never()).delete(any());
    }

    @Test
    void deleteAttachmentThrowsWhenNotUploaderOrOwner() {
        User uploader = activeUser(1L, "uploader@example.com", "uploader");
        User member = activeUser(2L, "member@example.com", "member");
        Project project = project(10L, uploader);
        Task task = task(100L, project, uploader);
        ProjectMember membership = membership(1000L, project, member, ProjectRole.MEMBER);
        TaskAttachment attachment = TaskAttachment.builder()
                .id(2000L)
                .task(task)
                .uploader(uploader)
                .originalFilename("report.pdf")
                .storedFilename("abc-report.pdf")
                .storagePath("task-attachments/10/abc-report.pdf")
                .contentType("application/pdf")
                .sizeBytes(5L)
                .build();

        given(taskAttachmentRepository.findById(2000L)).willReturn(Optional.of(attachment));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> taskAttachmentService.deleteAttachment(2L, 2000L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_PERMISSION);

        verify(taskAttachmentRepository, never()).delete(any(TaskAttachment.class));
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

    private Task task(Long id, Project project, User creator) {
        Task task = Task.create(
                project,
                creator,
                creator,
                "task",
                "desc",
                TaskPriority.MEDIUM,
                null,
                0
        );
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private ProjectMember membership(Long id, Project project, User user, ProjectRole role) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
    }

    private byte[] validPdfBytes() {
        return "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.UTF_8);
    }
}
