package com.taskflow.backend.domain.project.repository;

import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.ProjectTaskCountProjection;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WorkspaceProjectListingRepositoryTest extends IntegrationTestContainerSupport {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findAccessibleProjectsByWorkspaceAndUserReturnsUpdatedOrder() {
        User owner = persistUser("owner");
        User member = persistUser("member");
        Workspace workspace = persistWorkspace(owner, "workspace");

        Project olderProject = persistProject(workspace, owner, "older", LocalDateTime.now().minusMinutes(10));
        Project newerProject = persistProject(workspace, owner, "newer", LocalDateTime.now());
        persistProjectMember(olderProject, owner, ProjectRole.OWNER);
        persistProjectMember(newerProject, owner, ProjectRole.OWNER);
        persistProjectMember(newerProject, member, ProjectRole.MEMBER);

        entityManager.flush();
        entityManager.clear();

        List<ProjectMember> memberships =
                projectMemberRepository.findAllActiveByWorkspaceIdAndUserIdOrderByProjectUpdatedAtDesc(
                        workspace.getId(),
                        member.getId()
                );

        assertThat(memberships).hasSize(1);
        assertThat(memberships.get(0).getProject().getName()).isEqualTo("newer");
    }

    @Test
    void bulkCountQueriesReturnCountsPerProject() {
        User owner = persistUser("owner");
        Workspace workspace = persistWorkspace(owner, "workspace");

        Project alphaProject = persistProject(workspace, owner, "alpha", LocalDateTime.now().minusMinutes(5));
        Project betaProject = persistProject(workspace, owner, "beta", LocalDateTime.now());
        persistProjectMember(alphaProject, owner, ProjectRole.OWNER);
        persistProjectMember(betaProject, owner, ProjectRole.OWNER);
        persistProjectMember(alphaProject, persistUser("alpha-member"), ProjectRole.MEMBER);

        Task alphaTodo = persistTask(alphaProject, owner, "alpha-todo", TaskStatus.TODO, 0);
        Task alphaDone = persistTask(alphaProject, owner, "alpha-done", TaskStatus.DONE, 1);
        Task betaDone = persistTask(betaProject, owner, "beta-done", TaskStatus.DONE, 0);
        alphaDone.move(TaskStatus.DONE, 1, LocalDateTime.now());
        betaDone.move(TaskStatus.DONE, 0, LocalDateTime.now());
        alphaTodo.reassignPosition(0);

        entityManager.flush();
        entityManager.clear();

        Set<Long> projectIds = Set.of(alphaProject.getId(), betaProject.getId());

        Map<Long, Long> memberCounts = projectMemberRepository.countMembersByProjectIds(projectIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProjectMemberCountProjection::getProjectId,
                        ProjectMemberCountProjection::getMemberCount
                ));
        Map<Long, Long> taskCounts = taskRepository.countTasksByProjectIds(projectIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProjectTaskCountProjection::getProjectId,
                        ProjectTaskCountProjection::getTaskCount
                ));
        Map<Long, Long> doneTaskCounts = taskRepository.countDoneTasksByProjectIds(projectIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProjectTaskCountProjection::getProjectId,
                        ProjectTaskCountProjection::getTaskCount
                ));

        assertThat(memberCounts).containsEntry(alphaProject.getId(), 2L).containsEntry(betaProject.getId(), 1L);
        assertThat(taskCounts).containsEntry(alphaProject.getId(), 2L).containsEntry(betaProject.getId(), 1L);
        assertThat(doneTaskCounts).containsEntry(alphaProject.getId(), 1L).containsEntry(betaProject.getId(), 1L);
    }

    private User persistUser(String nicknamePrefix) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        String uniqueEmail = nicknamePrefix + "-" + uniqueSuffix + "@example.com";
        String uniqueNickname = trimNickname(nicknamePrefix + uniqueSuffix.substring(0, 8));
        return userRepository.save(User.builder()
                .email(uniqueEmail)
                .password("encoded")
                .nickname(uniqueNickname)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Workspace persistWorkspace(User owner, String name) {
        return workspaceRepository.save(Workspace.create(owner, name, name + " description"));
    }

    private Project persistProject(Workspace workspace, User owner, String name, LocalDateTime updatedAt) {
        Project project = projectRepository.save(Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name(name)
                .description(name + " description")
                .build());
        project.touch(updatedAt);
        return projectRepository.save(project);
    }

    private ProjectMember persistProjectMember(Project project, User user, ProjectRole role) {
        return projectMemberRepository.save(ProjectMember.create(project, user, role, LocalDateTime.now()));
    }

    private Task persistTask(Project project, User creator, String title, TaskStatus status, int position) {
        Task task = taskRepository.save(Task.create(
                project,
                creator,
                creator,
                title,
                title + " description",
                TaskPriority.MEDIUM,
                null,
                position
        ));
        if (status != TaskStatus.TODO) {
            task.move(status, position, LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    private String trimNickname(String nicknamePrefix) {
        return nicknamePrefix.length() <= 20 ? nicknamePrefix : nicknamePrefix.substring(0, 20);
    }
}
