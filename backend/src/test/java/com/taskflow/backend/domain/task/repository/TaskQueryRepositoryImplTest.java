package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.entity.TaskLabel;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.config.JpaConfig;
import com.taskflow.backend.global.config.QueryDslConfig;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaConfig.class, QueryDslConfig.class, TaskQueryRepositoryImpl.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskQueryRepositoryImplTest extends IntegrationTestContainerSupport {

    @Autowired
    private TaskQueryRepository taskQueryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findTasksSortsPriorityByBusinessRankAscending() {
        Project project = persistProjectGraph();
        persistTask(project, "task-low", TaskPriority.LOW, 0);
        persistTask(project, "task-medium", TaskPriority.MEDIUM, 1);
        persistTask(project, "task-high", TaskPriority.HIGH, 2);
        persistTask(project, "task-urgent", TaskPriority.URGENT, 3);
        entityManager.flush();
        entityManager.clear();

        Page<Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "priority",
                "ASC",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Task::getPriority)
                .containsExactly(TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.URGENT);
    }

    @Test
    void findTasksSortsPriorityByBusinessRankDescending() {
        Project project = persistProjectGraph();
        persistTask(project, "task-low", TaskPriority.LOW, 0);
        persistTask(project, "task-medium", TaskPriority.MEDIUM, 1);
        persistTask(project, "task-high", TaskPriority.HIGH, 2);
        persistTask(project, "task-urgent", TaskPriority.URGENT, 3);
        entityManager.flush();
        entityManager.clear();

        Page<Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "priority",
                "DESC",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Task::getPriority)
                .containsExactly(TaskPriority.URGENT, TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW);
    }

    @Test
    void findTasksTreatsWhitespaceWrappedDirectionAsAscending() {
        Project project = persistProjectGraph();
        persistTask(project, "task-urgent", TaskPriority.URGENT, 0);
        persistTask(project, "task-low", TaskPriority.LOW, 1);
        entityManager.flush();
        entityManager.clear();

        Page<Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "priority",
                "  ASC  ",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Task::getPriority)
                .containsExactly(TaskPriority.LOW, TaskPriority.URGENT);
    }

    @Test
    void findTasksReturnsEmptyPageWhenOffsetExceedsJpaLimit() {
        Project project = persistProjectGraph();
        persistTask(project, "task-low", TaskPriority.LOW, 0);
        entityManager.flush();
        entityManager.clear();

        Page<Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "updatedAt",
                "DESC",
                null,
                PageRequest.of(Integer.MAX_VALUE, Integer.MAX_VALUE)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void findTaskBoardTasksAppliesFiltersAndSortsByStatusAndPosition() {
        Project project = persistProjectGraph();
        User assignee = persistUser("assignee@example.com", "assignee");
        User otherAssignee = persistUser("other@example.com", "other");

        Label backendLabel = persistLabel(project, "backend", "#2563EB");
        Label designLabel = persistLabel(project, "design", "#F97316");

        Task todoFirst = persistTask(
                project,
                "API todo first",
                "desc",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                1,
                assignee
        );
        Task todoSecond = persistTask(
                project,
                "API todo second",
                "desc",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                2,
                assignee
        );
        Task doneTask = persistTask(
                project,
                "API done",
                "desc",
                TaskPriority.HIGH,
                TaskStatus.DONE,
                0,
                assignee
        );
        Task wrongAssignee = persistTask(
                project,
                "API wrong assignee",
                "desc",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                0,
                otherAssignee
        );
        Task wrongPriority = persistTask(
                project,
                "API wrong priority",
                "desc",
                TaskPriority.MEDIUM,
                TaskStatus.TODO,
                0,
                assignee
        );
        Task wrongKeyword = persistTask(
                project,
                "Board card",
                "non matching text",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                0,
                assignee
        );

        attachLabel(todoFirst, backendLabel);
        attachLabel(todoSecond, backendLabel);
        attachLabel(doneTask, backendLabel);
        attachLabel(wrongAssignee, backendLabel);
        attachLabel(wrongPriority, backendLabel);
        attachLabel(wrongKeyword, designLabel);

        entityManager.flush();
        entityManager.clear();

        List<Task> result = taskQueryRepository.findTaskBoardTasks(
                project.getId(),
                assignee.getId(),
                TaskPriority.HIGH,
                backendLabel.getId(),
                "api"
        );

        assertThat(result)
                .extracting(Task::getId)
                .containsExactly(todoFirst.getId(), todoSecond.getId(), doneTask.getId());
    }

    private Project persistProjectGraph() {
        User owner = User.builder()
                .email("owner@example.com")
                .password("encoded")
                .nickname("owner")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persist(owner);

        Workspace workspace = Workspace.create(owner, "ws", "workspace");
        entityManager.persist(workspace);

        Project project = Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name("project")
                .description("desc")
                .build();
        entityManager.persist(project);
        return project;
    }

    private User persistUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password("encoded")
                .nickname(nickname)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persist(user);
        return user;
    }

    private Label persistLabel(Project project, String name, String colorHex) {
        Label label = Label.builder()
                .project(project)
                .name(name)
                .colorHex(colorHex)
                .build();
        entityManager.persist(label);
        return label;
    }

    private void attachLabel(Task task, Label label) {
        entityManager.persist(TaskLabel.create(task, label));
    }

    private Task persistTask(Project project, String title, TaskPriority priority, int position) {
        return persistTask(project, title, "desc", priority, TaskStatus.TODO, position, null);
    }

    private Task persistTask(
            Project project,
            String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            int position,
            User assignee
    ) {
        Task task = Task.create(
                project,
                project.getOwner(),
                assignee,
                title,
                description,
                priority,
                null,
                position
        );
        if (status != TaskStatus.TODO) {
            task.move(status, position, LocalDateTime.of(2026, 3, 1, 9, 0));
        }
        entityManager.persist(task);
        return task;
    }
}
