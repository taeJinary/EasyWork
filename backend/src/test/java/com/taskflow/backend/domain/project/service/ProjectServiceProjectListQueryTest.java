package com.taskflow.backend.domain.project.service;

import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.project.dto.response.ProjectListResponse;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectQueryRepository;
import com.taskflow.backend.domain.project.repository.ProjectQueryRepository.ProjectListQueryResult;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceProjectListQueryTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectQueryRepository projectQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void getMyProjectsUsesDbPagingAndQueryFiltering() {
        User user = activeUser(1L);
        ProjectListQueryResult row = new ProjectListQueryResult(
                10L,
                "TaskFlow",
                "project",
                ProjectRole.OWNER,
                2L,
                4L,
                1L,
                LocalDateTime.of(2026, 3, 3, 20, 0)
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectQueryRepository.findMyProjects(1L, "task", ProjectRole.OWNER, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1L));

        ProjectListResponse response = projectService.getMyProjects(1L, 0, 20, " task ", ProjectRole.OWNER);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().projectId()).isEqualTo(10L);
        assertThat(response.content().getFirst().memberCount()).isEqualTo(2L);
        assertThat(response.content().getFirst().taskCount()).isEqualTo(4L);
        assertThat(response.content().getFirst().doneTaskCount()).isEqualTo(1L);
        assertThat(response.content().getFirst().progressRate()).isEqualTo(25);
        assertThat(response.totalElements()).isEqualTo(1L);
        verify(projectQueryRepository).findMyProjects(1L, "task", ProjectRole.OWNER, PageRequest.of(0, 20));
        verify(projectMemberRepository, never()).findAllActiveByUserIdOrderByProjectUpdatedAtDesc(1L);
    }

    @Test
    void getMyProjectsNormalizesNegativePageAndSize() {
        User user = activeUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(projectQueryRepository.findMyProjects(1L, null, null, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        ProjectListResponse response = projectService.getMyProjects(1L, -1, -100, null, null);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.content()).isEmpty();
        verify(projectQueryRepository).findMyProjects(1L, null, null, PageRequest.of(0, 20));
    }

    private User activeUser(Long userId) {
        return User.builder()
                .id(userId)
                .email("user@example.com")
                .password("encoded")
                .nickname("tester")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
