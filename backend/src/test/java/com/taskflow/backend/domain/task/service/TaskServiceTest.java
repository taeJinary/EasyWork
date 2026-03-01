package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.label.repository.LabelRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskLabelRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskLabelRepository taskLabelRepository;

    @Mock
    private LabelRepository labelRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskCreatesTodoAtBottomForProjectMember() {
        User creator = activeUser(1L, "owner@example.com", "오너");
        User assignee = activeUser(2L, "member@example.com", "팀원");

        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember creatorMembership = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(creator)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember assigneeMembership = ProjectMember.builder()
                .id(102L)
                .project(project)
                .user(assignee)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();
        Label backendLabel = Label.builder()
                .id(1L)
                .project(project)
                .name("백엔드")
                .colorHex("#2563EB")
                .build();
        Label authLabel = Label.builder()
                .id(2L)
                .project(project)
                .name("인증")
                .colorHex("#16A34A")
                .build();

        CreateTaskRequest request = new CreateTaskRequest(
                "로그인 API 구현",
                "Access/Refresh 구조 구현",
                2L,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 10),
                List.of(1L, 2L)
        );

        Task savedTask = Task.builder()
                .id(100L)
                .project(project)
                .creator(creator)
                .assignee(assignee)
                .title("로그인 API 구현")
                .description("Access/Refresh 구조 구현")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(0L)
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(creatorMembership));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(assigneeMembership));
        given(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(10L, TaskStatus.TODO)).willReturn(0L);
        given(taskRepository.save(any(Task.class))).willReturn(savedTask);
        given(labelRepository.findAllByIdInAndProjectId(List.of(1L, 2L), 10L)).willReturn(List.of(backendLabel, authLabel));

        TaskSummaryResponse response = taskService.createTask(1L, 10L, request);

        assertThat(response.taskId()).isEqualTo(100L);
        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("로그인 API 구현");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.position()).isEqualTo(0);
        assertThat(response.version()).isEqualTo(0L);
        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().userId()).isEqualTo(2L);
        verify(taskLabelRepository).saveAll(anyList());
    }

    @Test
    void createTaskThrowsWhenCreatorIsNotProjectMember() {
        User creator = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("설명")
                .build();

        CreateTaskRequest request = new CreateTaskRequest(
                "로그인 API 구현",
                "설명",
                null,
                TaskPriority.MEDIUM,
                null,
                List.of()
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTaskThrowsWhenAssigneeIsNotProjectMember() {
        User creator = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember creatorMembership = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(creator)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        CreateTaskRequest request = new CreateTaskRequest(
                "로그인 API 구현",
                "설명",
                99L,
                TaskPriority.MEDIUM,
                null,
                List.of()
        );

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(creatorMembership));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void createTaskThrowsWhenLabelNotFound() {
        User creator = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember creatorMembership = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(creator)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        CreateTaskRequest request = new CreateTaskRequest(
                "로그인 API 구현",
                "설명",
                null,
                TaskPriority.HIGH,
                null,
                List.of(1L, 2L)
        );

        Task savedTask = Task.builder()
                .id(100L)
                .project(project)
                .creator(creator)
                .assignee(null)
                .title("로그인 API 구현")
                .description("설명")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(null)
                .position(0)
                .version(0L)
                .build();

        Label backendLabel = Label.builder()
                .id(1L)
                .project(project)
                .name("백엔드")
                .colorHex("#2563EB")
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(creatorMembership));
        given(taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(10L, TaskStatus.TODO)).willReturn(0L);
        given(taskRepository.save(any(Task.class))).willReturn(savedTask);
        given(labelRepository.findAllByIdInAndProjectId(List.of(1L, 2L), 10L)).willReturn(List.of(backendLabel));

        assertThatThrownBy(() -> taskService.createTask(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABEL_NOT_FOUND);

        verify(taskLabelRepository, never()).saveAll(anyList());
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
