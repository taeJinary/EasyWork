package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListItemResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberCountProjection;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
        given(workspaceMemberRepository.findByUserIdOrderByWorkspaceUpdatedAtDesc(
                1L,
                PageRequest.of(0, 20)
        )).willReturn(new PageImpl<>(List.of(membership), PageRequest.of(0, 20), 1));
        given(workspaceMemberRepository.countMembersByWorkspaceIds(Set.of(10L)))
                .willReturn(List.of(countProjection(10L, 1L)));

        WorkspaceListResponse response = workspaceService.getMyWorkspaces(1L, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().workspaceId()).isEqualTo(10L);
        assertThat(response.content().getFirst().myRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(response.content().getFirst().memberCount()).isEqualTo(1L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(1L);
        verify(workspaceMemberRepository, never()).findAllByUserIdOrderByWorkspaceUpdatedAtDesc(any(Long.class));
        verify(workspaceMemberRepository, never()).countByWorkspaceId(any(Long.class));
    }

    @Test
    void getMyWorkspacesHandlesLargePageWithoutOverflowCalculation() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceMemberRepository.findByUserIdOrderByWorkspaceUpdatedAtDesc(
                1L,
                PageRequest.of(Integer.MAX_VALUE, 100)
        )).willReturn(Page.empty(PageRequest.of(Integer.MAX_VALUE, 100)));

        WorkspaceListResponse response = workspaceService.getMyWorkspaces(1L, Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(Integer.MAX_VALUE);
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.last()).isTrue();
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

    private WorkspaceMemberCountProjection countProjection(Long workspaceId, long memberCount) {
        return new WorkspaceMemberCountProjection() {
            @Override
            public Long getWorkspaceId() {
                return workspaceId;
            }

            @Override
            public long getMemberCount() {
                return memberCount;
            }
        };
    }
}
