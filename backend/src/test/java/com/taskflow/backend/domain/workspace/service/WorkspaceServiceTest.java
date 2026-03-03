package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspaceCreatesOwnerMembership() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("TaskFlow Team", "team workspace");
        Workspace savedWorkspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceRepository.save(any(Workspace.class))).willReturn(savedWorkspace);

        WorkspaceSummaryResponse response = workspaceService.createWorkspace(1L, request);

        assertThat(response.workspaceId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("TaskFlow Team");
        assertThat(response.myRole()).isEqualTo(WorkspaceRole.OWNER);
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void getMyWorkspacesReturnsPagedResponse() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        WorkspaceMember membership = WorkspaceMember.builder()
                .id(100L)
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceMemberRepository.findAllByUserIdOrderByWorkspaceUpdatedAtDesc(1L))
                .willReturn(List.of(membership));
        given(workspaceMemberRepository.countByWorkspaceId(10L)).willReturn(1L);

        WorkspaceListResponse response = workspaceService.getMyWorkspaces(1L, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().workspaceId()).isEqualTo(10L);
        assertThat(response.content().getFirst().myRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(response.content().getFirst().memberCount()).isEqualTo(1L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(1L);
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
