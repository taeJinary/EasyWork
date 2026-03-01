package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.label.repository.LabelRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.entity.TaskLabel;
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

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(taskLabelRepository.findAllByTaskIdInWithLabel(List.of(1000L)))
                .willReturn(List.of(TaskLabel.create(task, backendLabel)));

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
        assertThat(response.recentStatusHistories()).isEmpty();
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
