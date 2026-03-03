package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.project.entity.Project;
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

        Page<com.taskflow.backend.domain.task.entity.Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "priority",
                "ASC",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(com.taskflow.backend.domain.task.entity.Task::getPriority)
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

        Page<com.taskflow.backend.domain.task.entity.Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "priority",
                "DESC",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(com.taskflow.backend.domain.task.entity.Task::getPriority)
                .containsExactly(TaskPriority.URGENT, TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW);
    }

    @Test
    void findTasksTreatsWhitespaceWrappedDirectionAsAscending() {
        Project project = persistProjectGraph();
        persistTask(project, "task-urgent", TaskPriority.URGENT, 0);
        persistTask(project, "task-low", TaskPriority.LOW, 1);
        entityManager.flush();
        entityManager.clear();

        Page<com.taskflow.backend.domain.task.entity.Task> result = taskQueryRepository.findTasks(
                project.getId(),
                TaskStatus.TODO,
                "priority",
                "  ASC  ",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(com.taskflow.backend.domain.task.entity.Task::getPriority)
                .containsExactly(TaskPriority.LOW, TaskPriority.URGENT);
    }

    @Test
    void findTasksReturnsEmptyPageWhenOffsetExceedsJpaLimit() {
        Project project = persistProjectGraph();
        persistTask(project, "task-low", TaskPriority.LOW, 0);
        entityManager.flush();
        entityManager.clear();

        Page<com.taskflow.backend.domain.task.entity.Task> result = taskQueryRepository.findTasks(
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

    private void persistTask(Project project, String title, TaskPriority priority, int position) {
        com.taskflow.backend.domain.task.entity.Task task = com.taskflow.backend.domain.task.entity.Task.create(
                project,
                project.getOwner(),
                null,
                title,
                "desc",
                priority,
                null,
                position
        );
        entityManager.persist(task);
    }
}
