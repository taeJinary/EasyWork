package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.label.repository.LabelRepository;
import com.taskflow.backend.domain.notification.service.NotificationService;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.request.MoveTaskRequest;
import com.taskflow.backend.domain.task.dto.request.UpdateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.entity.TaskLabel;
import com.taskflow.backend.domain.task.entity.TaskStatusHistory;
import com.taskflow.backend.domain.task.repository.TaskLabelRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.task.repository.TaskStatusHistoryRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.websocket.ProjectBoardEventPublisher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private TaskStatusHistoryRepository taskStatusHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProjectBoardEventPublisher projectBoardEventPublisher;

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
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

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
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
        verify(notificationService).createTaskAssignedNotification(savedTask, creator);
        verify(taskLabelRepository).saveAll(anyList());
        verify(projectBoardEventPublisher).publishTaskCreated(savedTask, creator);
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

    @Test
    void getTaskBoardReturnsColumnsWithFilteredCards() {
        User member = activeUser(1L, "member@example.com", "멤버");
        User assignee = activeUser(2L, "assignee@example.com", "담당자");

        Project project = Project.builder()
                .id(10L)
                .owner(member)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(member)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        Task todoTask = Task.builder()
                .id(1000L)
                .project(project)
                .creator(member)
                .assignee(assignee)
                .title("로그인 API 구현")
                .description("Access/Refresh 구조 구현")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(0L)
                .build();
        Task inProgressTask = Task.builder()
                .id(1001L)
                .project(project)
                .creator(member)
                .assignee(assignee)
                .title("초대 API 구현")
                .description("메일 초대 흐름")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 3, 11))
                .position(0)
                .version(1L)
                .build();

        Label backendLabel = Label.builder()
                .id(1L)
                .project(project)
                .name("백엔드")
                .colorHex("#2563EB")
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskRepository.findAllByProjectIdAndDeletedAtIsNullOrderByStatusAscPositionAsc(10L))
                .willReturn(List.of(todoTask, inProgressTask));
        given(taskLabelRepository.findAllByTaskIdInWithLabel(List.of(1000L, 1001L)))
                .willReturn(List.of(
                        TaskLabel.create(todoTask, backendLabel),
                        TaskLabel.create(inProgressTask, backendLabel)
                ));

        TaskBoardResponse response = taskService.getTaskBoard(1L, 10L, 2L, TaskPriority.HIGH, 1L, "API");

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.filters().assigneeUserId()).isEqualTo(2L);
        assertThat(response.filters().priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.filters().labelId()).isEqualTo(1L);
        assertThat(response.filters().keyword()).isEqualTo("API");

        TaskBoardResponse.ColumnResponse todoColumn = response.columns().stream()
                .filter(column -> column.status() == TaskStatus.TODO)
                .findFirst()
                .orElseThrow();
        TaskBoardResponse.ColumnResponse inProgressColumn = response.columns().stream()
                .filter(column -> column.status() == TaskStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow();
        TaskBoardResponse.ColumnResponse doneColumn = response.columns().stream()
                .filter(column -> column.status() == TaskStatus.DONE)
                .findFirst()
                .orElseThrow();

        assertThat(todoColumn.tasks()).hasSize(1);
        assertThat(todoColumn.tasks().getFirst().taskId()).isEqualTo(1000L);
        assertThat(todoColumn.tasks().getFirst().labels()).hasSize(1);
        assertThat(todoColumn.tasks().getFirst().commentCount()).isEqualTo(0L);
        assertThat(inProgressColumn.tasks()).hasSize(1);
        assertThat(inProgressColumn.tasks().getFirst().taskId()).isEqualTo(1001L);
        assertThat(doneColumn.tasks()).isEmpty();
    }

    @Test
    void getTaskBoardThrowsWhenNotProjectMember() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskBoard(1L, 10L, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);
    }

    @Test
    void getTasksReturnsPagedAndSortedContent() {
        User member = activeUser(1L, "member@example.com", "멤버");
        User assignee = activeUser(2L, "assignee@example.com", "담당자");

        Project project = Project.builder()
                .id(10L)
                .owner(member)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(member)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        Task task1 = Task.builder()
                .id(1000L)
                .project(project)
                .creator(member)
                .assignee(assignee)
                .title("로그인 API 구현")
                .description("Access/Refresh 구조 구현")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 3, 11))
                .position(0)
                .version(0L)
                .build();
        Task task2 = Task.builder()
                .id(1001L)
                .project(project)
                .creator(member)
                .assignee(assignee)
                .title("초대 API 구현")
                .description("메일 초대 흐름")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(1)
                .version(1L)
                .build();
        Task task3 = Task.builder()
                .id(1002L)
                .project(project)
                .creator(member)
                .assignee(null)
                .title("보드 UI 점검")
                .description("프론트 체크")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.LOW)
                .dueDate(null)
                .position(0)
                .version(2L)
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskRepository.findAllByProjectIdAndDeletedAtIsNullOrderByStatusAscPositionAsc(10L))
                .willReturn(List.of(task1, task2, task3));

        TaskListResponse response = taskService.getTasks(
                1L,
                10L,
                0,
                20,
                TaskStatus.TODO,
                "dueDate",
                "ASC",
                "API"
        );

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().taskId()).isEqualTo(1001L);
        assertThat(response.content().get(1).taskId()).isEqualTo(1000L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2L);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void getTasksMatchesKeywordIndependentlyOfDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            User member = activeUser(1L, "member@example.com", "member");
            Project project = Project.builder()
                    .id(10L)
                    .owner(member)
                    .name("TaskFlow")
                    .description("desc")
                    .build();
            ProjectMember membership = ProjectMember.builder()
                    .id(100L)
                    .project(project)
                    .user(member)
                    .role(ProjectRole.MEMBER)
                    .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                    .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                    .build();
            Task task = Task.builder()
                    .id(1000L)
                    .project(project)
                    .creator(member)
                    .assignee(null)
                    .title("Istanbul integration")
                    .description("keyword matching regression")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .dueDate(null)
                    .position(0)
                    .version(0L)
                    .build();

            given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
            given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
            given(taskRepository.findAllByProjectIdAndDeletedAtIsNullOrderByStatusAscPositionAsc(10L))
                    .willReturn(List.of(task));

            TaskListResponse response = taskService.getTasks(
                    1L,
                    10L,
                    0,
                    20,
                    TaskStatus.TODO,
                    "updatedAt",
                    "DESC",
                    "istanbul"
            );

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().getFirst().taskId()).isEqualTo(1000L);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void getTaskBoardMatchesKeywordIndependentlyOfDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            User member = activeUser(1L, "member@example.com", "member");
            Project project = Project.builder()
                    .id(10L)
                    .owner(member)
                    .name("TaskFlow")
                    .description("desc")
                    .build();
            ProjectMember membership = ProjectMember.builder()
                    .id(100L)
                    .project(project)
                    .user(member)
                    .role(ProjectRole.MEMBER)
                    .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                    .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                    .build();
            Task task = Task.builder()
                    .id(1000L)
                    .project(project)
                    .creator(member)
                    .assignee(null)
                    .title("Istanbul board")
                    .description("keyword matching regression")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .dueDate(null)
                    .position(0)
                    .version(0L)
                    .build();

            given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
            given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
            given(taskRepository.findAllByProjectIdAndDeletedAtIsNullOrderByStatusAscPositionAsc(10L))
                    .willReturn(List.of(task));
            given(taskLabelRepository.findAllByTaskIdInWithLabel(List.of(1000L))).willReturn(List.of());

            TaskBoardResponse response = taskService.getTaskBoard(1L, 10L, null, null, null, "istanbul");
            TaskBoardResponse.ColumnResponse todoColumn = response.columns().stream()
                    .filter(column -> column.status() == TaskStatus.TODO)
                    .findFirst()
                    .orElseThrow();

            assertThat(todoColumn.tasks()).hasSize(1);
            assertThat(todoColumn.tasks().getFirst().taskId()).isEqualTo(1000L);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void getTasksThrowsWhenNotProjectMember() {
        User owner = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("설명")
                .build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTasks(1L, 10L, 0, 20, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);
    }

    @Test
    void getTaskDetailReturnsMetadataForProjectMember() {
        User creator = activeUser(1L, "owner@example.com", "오너");
        User assignee = activeUser(2L, "member@example.com", "팀원");

        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(creator)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        Task task = Task.builder()
                .id(1000L)
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
        Label backendLabel = Label.builder()
                .id(1L)
                .project(project)
                .name("백엔드")
                .colorHex("#2563EB")
                .build();

        TaskStatusHistory history = TaskStatusHistory.create(
                task,
                TaskStatus.TODO,
                TaskStatus.IN_PROGRESS,
                assignee
        );
        ReflectionTestUtils.setField(history, "id", 200L);
        ReflectionTestUtils.setField(history, "createdAt", LocalDateTime.of(2026, 3, 2, 9, 0));

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskLabelRepository.findAllByTaskIdInWithLabel(List.of(1000L)))
                .willReturn(List.of(TaskLabel.create(task, backendLabel)));
        given(taskStatusHistoryRepository.findTop10ByTaskIdOrderByCreatedAtDesc(1000L))
                .willReturn(List.of(history));

        TaskDetailResponse response = taskService.getTaskDetail(1L, 1000L);

        assertThat(response.taskId()).isEqualTo(1000L);
        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("로그인 API 구현");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.creator().userId()).isEqualTo(1L);
        assertThat(response.assignee().userId()).isEqualTo(2L);
        assertThat(response.labels()).hasSize(1);
        assertThat(response.labels().getFirst().labelId()).isEqualTo(1L);
        assertThat(response.commentCount()).isEqualTo(0L);
        assertThat(response.recentStatusHistories()).hasSize(1);
        assertThat(response.recentStatusHistories().getFirst().historyId()).isEqualTo(200L);
        assertThat(response.recentStatusHistories().getFirst().fromStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.recentStatusHistories().getFirst().toStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.recentStatusHistories().getFirst().changedBy().userId()).isEqualTo(2L);
        assertThat(response.recentStatusHistories().getFirst().changedAt())
                .isEqualTo(LocalDateTime.of(2026, 3, 2, 9, 0));
    }

    @Test
    void getTaskDetailThrowsWhenTaskNotFound() {
        given(taskRepository.findByIdAndDeletedAtIsNull(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskDetail(1L, 9999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @Test
    void getTaskDetailThrowsWhenNotProjectMember() {
        User creator = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("설명")
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(creator)
                .assignee(null)
                .title("로그인 API 구현")
                .description("설명")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(0L)
                .build();

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskDetail(2L, 1000L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);
    }

    @Test
    void updateTaskUpdatesFieldsAndSyncsLabelsWhenVersionMatches() {
        User actor = activeUser(1L, "owner@example.com", "오너");
        User assignee = activeUser(2L, "member@example.com", "팀원");

        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember actorMembership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember assigneeMembership = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(assignee)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();

        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("기존 제목")
                .description("기존 설명")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(0L)
                .build();

        Label label2 = Label.builder()
                .id(2L)
                .project(project)
                .name("백엔드")
                .colorHex("#2563EB")
                .build();
        Label label3 = Label.builder()
                .id(3L)
                .project(project)
                .name("인증")
                .colorHex("#16A34A")
                .build();

        UpdateTaskRequest request = new UpdateTaskRequest(
                "로그인 API 및 재발급 구현",
                "설명을 수정했습니다.",
                2L,
                TaskPriority.URGENT,
                LocalDate.of(2026, 3, 11),
                List.of(2L, 3L),
                0L
        );
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(actorMembership));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(assigneeMembership));
        given(labelRepository.findAllByIdInAndProjectId(List.of(2L, 3L), 10L)).willReturn(List.of(label2, label3));
        given(taskLabelRepository.findAllByTaskIdInWithLabel(List.of(1000L)))
                .willReturn(List.of(TaskLabel.create(task, label2), TaskLabel.create(task, label3)));

        TaskDetailResponse response = taskService.updateTask(1L, 1000L, request);

        assertThat(task.getTitle()).isEqualTo("로그인 API 및 재발급 구현");
        assertThat(task.getDescription()).isEqualTo("설명을 수정했습니다.");
        assertThat(task.getAssignee()).isEqualTo(assignee);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.URGENT);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 11));

        assertThat(response.taskId()).isEqualTo(1000L);
        assertThat(response.title()).isEqualTo("로그인 API 및 재발급 구현");
        assertThat(response.priority()).isEqualTo(TaskPriority.URGENT);
        assertThat(response.labels()).hasSize(2);
        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().userId()).isEqualTo(2L);
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);

        verify(taskLabelRepository).deleteAllByTaskId(1000L);
        verify(taskLabelRepository).saveAll(anyList());
        verify(notificationService).createTaskAssignedNotification(task, actor);
        verify(projectBoardEventPublisher).publishTaskUpdated(task, actor);
    }

    @Test
    void updateTaskThrowsWhenVersionConflict() {
        User actor = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember actorMembership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("기존 제목")
                .description("기존 설명")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(1L)
                .build();
        UpdateTaskRequest request = new UpdateTaskRequest(
                "새 제목",
                "새 설명",
                null,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 11),
                List.of(),
                0L
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(actorMembership));

        assertThatThrownBy(() -> taskService.updateTask(1L, 1000L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_CONFLICT);

        verify(taskLabelRepository, never()).deleteAllByTaskId(1000L);
        verify(taskLabelRepository, never()).saveAll(anyList());
    }

    @Test
    void updateTaskThrowsWhenAssigneeNotProjectMember() {
        User actor = activeUser(1L, "owner@example.com", "오너");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember actorMembership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("기존 제목")
                .description("기존 설명")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(0L)
                .build();
        UpdateTaskRequest request = new UpdateTaskRequest(
                "새 제목",
                "새 설명",
                99L,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 11),
                List.of(),
                0L
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(actorMembership));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(1L, 1000L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void updateTaskThrowsWhenLabelNotFound() {
        User actor = activeUser(1L, "owner@example.com", "오너");
        User assignee = activeUser(2L, "member@example.com", "팀원");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("설명")
                .build();
        ProjectMember actorMembership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        ProjectMember assigneeMembership = ProjectMember.builder()
                .id(101L)
                .project(project)
                .user(assignee)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 30))
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("기존 제목")
                .description("기존 설명")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 3, 10))
                .position(0)
                .version(0L)
                .build();
        Label label2 = Label.builder()
                .id(2L)
                .project(project)
                .name("백엔드")
                .colorHex("#2563EB")
                .build();
        UpdateTaskRequest request = new UpdateTaskRequest(
                "새 제목",
                "새 설명",
                2L,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 11),
                List.of(2L, 3L),
                0L
        );

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(actorMembership));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 2L)).willReturn(Optional.of(assigneeMembership));
        given(labelRepository.findAllByIdInAndProjectId(List.of(2L, 3L), 10L)).willReturn(List.of(label2));

        assertThatThrownBy(() -> taskService.updateTask(1L, 1000L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABEL_NOT_FOUND);

        verify(taskLabelRepository, never()).deleteAllByTaskId(1000L);
        verify(taskLabelRepository, never()).saveAll(anyList());
    }

    @Test
    void moveTaskReordersWithinSameStatusColumn() {
        User actor = activeUser(1L, "owner@example.com", "owner");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        Task taskA = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task A")
                .description("A")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(0)
                .version(0L)
                .build();
        Task taskB = Task.builder()
                .id(1001L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task B")
                .description("B")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(1)
                .version(0L)
                .build();
        Task taskC = Task.builder()
                .id(1002L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task C")
                .description("C")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(2)
                .version(0L)
                .build();

        MoveTaskRequest request = new MoveTaskRequest(TaskStatus.TODO, 0, 0L);
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(taskB));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(10L, TaskStatus.TODO))
                .willReturn(List.of(taskA, taskB, taskC));

        var response = taskService.moveTask(1L, 1001L, request);

        assertThat(response.taskId()).isEqualTo(1001L);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.position()).isEqualTo(0);
        assertThat(taskB.getPosition()).isEqualTo(0);
        assertThat(taskA.getPosition()).isEqualTo(1);
        assertThat(taskC.getPosition()).isEqualTo(2);
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
        verify(taskStatusHistoryRepository, never()).save(any(TaskStatusHistory.class));
    }

    @Test
    void moveTaskChangesStatusAndCompletedAtWhenMovingToDone() {
        User actor = activeUser(1L, "owner@example.com", "owner");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        Task movingTask = Task.builder()
                .id(1001L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task B")
                .description("B")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(1)
                .version(0L)
                .completedAt(null)
                .build();
        Task sourceTask = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task A")
                .description("A")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(0)
                .version(0L)
                .build();
        Task doneTask = Task.builder()
                .id(1002L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task C")
                .description("C")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.HIGH)
                .dueDate(null)
                .position(0)
                .version(0L)
                .completedAt(LocalDateTime.of(2026, 3, 1, 12, 0))
                .build();

        MoveTaskRequest request = new MoveTaskRequest(TaskStatus.DONE, 1, 0L);

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(movingTask));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(10L, TaskStatus.TODO))
                .willReturn(List.of(sourceTask, movingTask));
        given(taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(10L, TaskStatus.DONE))
                .willReturn(List.of(doneTask));

        var response = taskService.moveTask(1L, 1001L, request);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.completedAt()).isNotNull();
        assertThat(movingTask.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(movingTask.getPosition()).isEqualTo(1);
        assertThat(movingTask.getCompletedAt()).isNotNull();
        assertThat(sourceTask.getPosition()).isEqualTo(0);
        assertThat(doneTask.getPosition()).isEqualTo(0);

        ArgumentCaptor<TaskStatusHistory> historyCaptor = ArgumentCaptor.forClass(TaskStatusHistory.class);
        verify(taskStatusHistoryRepository).save(historyCaptor.capture());

        TaskStatusHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getTask()).isEqualTo(movingTask);
        assertThat(savedHistory.getFromStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(savedHistory.getToStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(savedHistory.getChangedBy()).isEqualTo(actor);
        verify(projectBoardEventPublisher).publishTaskMoved(
                movingTask,
                actor,
                TaskStatus.TODO,
                TaskStatus.DONE
        );
    }

    @Test
    void moveTaskClearsCompletedAtWhenMovingOutOfDone() {
        User actor = activeUser(1L, "owner@example.com", "owner");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();

        Task movingTask = Task.builder()
                .id(1001L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Done task")
                .description("done")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(0)
                .version(0L)
                .completedAt(LocalDateTime.of(2026, 3, 1, 12, 0))
                .build();
        Task todoTask = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Todo task")
                .description("todo")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(0)
                .version(0L)
                .build();

        MoveTaskRequest request = new MoveTaskRequest(TaskStatus.TODO, 1, 0L);

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(movingTask));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(10L, TaskStatus.DONE))
                .willReturn(List.of(movingTask));
        given(taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(10L, TaskStatus.TODO))
                .willReturn(List.of(todoTask));

        var response = taskService.moveTask(1L, 1001L, request);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.completedAt()).isNull();
        assertThat(movingTask.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(movingTask.getCompletedAt()).isNull();
        assertThat(todoTask.getPosition()).isEqualTo(0);
    }

    @Test
    void deleteTaskSoftDeletesWhenRequesterIsCreator() {
        User actor = activeUser(1L, "member@example.com", "member");
        User owner = activeUser(9L, "owner@example.com", "owner");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1001L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task B")
                .description("B")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(1)
                .version(0L)
                .deletedAt(null)
                .build();
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        taskService.deleteTask(1L, 1001L);

        assertThat(task.getDeletedAt()).isNotNull();
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
        verify(projectBoardEventPublisher).publishTaskDeleted(task, actor);
    }

    @Test
    void deleteTaskSoftDeletesWhenRequesterIsOwner() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User creator = activeUser(2L, "member@example.com", "member");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(owner)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1001L)
                .project(project)
                .creator(creator)
                .assignee(null)
                .title("Task B")
                .description("B")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(1)
                .version(0L)
                .deletedAt(null)
                .build();

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        taskService.deleteTask(1L, 1001L);

        assertThat(task.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteTaskThrowsWhenRequesterIsNeitherCreatorNorOwner() {
        User actor = activeUser(1L, "member@example.com", "member");
        User owner = activeUser(9L, "owner@example.com", "owner");
        User creator = activeUser(2L, "creator@example.com", "creator");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1001L)
                .project(project)
                .creator(creator)
                .assignee(null)
                .title("Task B")
                .description("B")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(1)
                .version(0L)
                .deletedAt(null)
                .build();

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> taskService.deleteTask(1L, 1001L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_PERMISSION);
    }

    @Test
    void deleteTaskThrowsWhenTaskNotFound() {
        given(taskRepository.findByIdAndDeletedAtIsNull(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(1L, 9999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @Test
    void moveTaskThrowsWhenVersionConflict() {
        User actor = activeUser(1L, "owner@example.com", "owner");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task movingTask = Task.builder()
                .id(1001L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("Task B")
                .description("B")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(null)
                .position(1)
                .version(1L)
                .build();

        MoveTaskRequest request = new MoveTaskRequest(TaskStatus.DONE, 0, 0L);

        given(taskRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(movingTask));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        assertThatThrownBy(() -> taskService.moveTask(1L, 1001L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_CONFLICT);
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
