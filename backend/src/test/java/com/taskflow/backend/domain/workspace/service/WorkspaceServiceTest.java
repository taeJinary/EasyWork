package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListItemResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceMemberResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberCountProjection;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ProjectRepository projectRepository;

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

    @Test
    void getWorkspaceDetailReturnsSummaryWhenRequesterIsMember() {
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
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(workspaceMemberRepository.countByWorkspaceId(10L)).willReturn(2L);

        WorkspaceDetailResponse response = workspaceService.getWorkspaceDetail(1L, 10L);

        assertThat(response.workspaceId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("TaskFlow Team");
        assertThat(response.myRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(response.memberCount()).isEqualTo(2L);
    }

    @Test
    void getWorkspaceDetailThrowsWhenRequesterIsNotWorkspaceMember() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.getWorkspaceDetail(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getWorkspaceMembersReturnsMemberListWhenRequesterIsMember() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User member = activeUser(2L, "member@example.com", "member");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        WorkspaceMember requesterMembership = WorkspaceMember.builder()
                .id(100L)
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build();
        WorkspaceMember memberMembership = WorkspaceMember.builder()
                .id(101L)
                .workspace(workspace)
                .user(member)
                .role(WorkspaceRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 10, 0))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                .willReturn(Optional.of(requesterMembership));
        given(workspaceMemberRepository.findAllWithUserByWorkspaceIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(requesterMembership, memberMembership));

        List<WorkspaceMemberResponse> response = workspaceService.getWorkspaceMembers(1L, 10L);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().userId()).isEqualTo(1L);
        assertThat(response.get(1).userId()).isEqualTo(2L);
        assertThat(response.get(1).role()).isEqualTo(WorkspaceRole.MEMBER);
        verify(workspaceMemberRepository).findAllWithUserByWorkspaceIdOrderByJoinedAtAsc(10L);
        verify(workspaceMemberRepository, never()).findAllByWorkspaceIdOrderByJoinedAtAsc(any(Long.class));
    }

    @Test
    void updateWorkspaceUpdatesWorkspaceWhenRequesterIsOwner() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        WorkspaceMember ownerMembership = WorkspaceMember.builder()
                .id(100L)
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build();
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest("TaskFlow Core", "core workspace");

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                .willReturn(Optional.of(ownerMembership));

        WorkspaceSummaryResponse response = workspaceService.updateWorkspace(1L, 10L, request);

        assertThat(response.workspaceId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("TaskFlow Core");
        assertThat(response.description()).isEqualTo("core workspace");
        assertThat(response.myRole()).isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    void updateWorkspaceThrowsWhenRequesterIsNotOwner() {
        User member = activeUser(2L, "member@example.com", "member");
        User owner = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        WorkspaceMember memberMembership = WorkspaceMember.builder()
                .id(101L)
                .workspace(workspace)
                .user(member)
                .role(WorkspaceRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 10, 0))
                .build();
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest("TaskFlow Core", "core workspace");

        given(userRepository.findById(2L)).willReturn(Optional.of(member));
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> workspaceService.updateWorkspace(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);
    }

    @Test
    void deleteWorkspaceDeletesMembershipsAndWorkspaceWhenRequesterIsOwner() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        WorkspaceMember ownerMembership = WorkspaceMember.builder()
                .id(100L)
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build();
        Project project = Project.builder()
                .id(20L)
                .workspace(workspace)
                .owner(owner)
                .name("TaskFlow Project")
                .description("project")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                .willReturn(Optional.of(ownerMembership));
        given(projectRepository.findAllByWorkspaceIdAndDeletedAtIsNull(10L)).willReturn(List.of(project));

        workspaceService.deleteWorkspace(1L, 10L);

        assertThat(project.isDeleted()).isTrue();
        verify(projectRepository).findAllByWorkspaceIdAndDeletedAtIsNull(10L);
        verify(workspaceMemberRepository).deleteAllByWorkspaceId(10L);
        verify(workspaceRepository).delete(workspace);
    }

    @Test
    void deleteWorkspaceThrowsWhenRequesterIsNotOwner() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User member = activeUser(2L, "member@example.com", "member");
        Workspace workspace = Workspace.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow Team")
                .description("team workspace")
                .build();
        WorkspaceMember memberMembership = WorkspaceMember.builder()
                .id(101L)
                .workspace(workspace)
                .user(member)
                .role(WorkspaceRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 3, 10, 0))
                .build();

        given(userRepository.findById(2L)).willReturn(Optional.of(member));
        given(workspaceRepository.findById(10L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L))
                .willReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> workspaceService.deleteWorkspace(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);
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
