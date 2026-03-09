package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectListItemResponse;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceMemberResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkspaceServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getMyWorkspacesReturnsUpdatedOrderAndMemberCounts() {
        User owner = saveActiveUser("ws-owner");
        User member = saveActiveUser("ws-member");

        WorkspaceSummaryResponse olderWorkspace = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("alpha-workspace", "alpha description")
        );
        WorkspaceSummaryResponse newerWorkspace = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("beta-workspace", "beta description")
        );

        Workspace alpha = workspaceRepository.findById(olderWorkspace.workspaceId()).orElseThrow();
        alpha.touch(LocalDateTime.now().minusMinutes(10));
        workspaceRepository.saveAndFlush(alpha);

        Workspace beta = workspaceRepository.findById(newerWorkspace.workspaceId()).orElseThrow();
        beta.touch(LocalDateTime.now());
        workspaceRepository.saveAndFlush(beta);

        workspaceMemberRepository.save(WorkspaceMember.create(
                alpha,
                member,
                WorkspaceRole.MEMBER,
                LocalDateTime.now()
        ));

        WorkspaceListResponse response = workspaceService.getMyWorkspaces(owner.getId(), 0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).workspaceId()).isEqualTo(beta.getId());
        assertThat(response.content().get(0).memberCount()).isEqualTo(1L);
        assertThat(response.content().get(1).workspaceId()).isEqualTo(alpha.getId());
        assertThat(response.content().get(1).memberCount()).isEqualTo(2L);
    }

    @Test
    void getWorkspaceDetailAndMembersReturnRoleCountAndJoinedOrder() {
        User owner = saveActiveUser("ws-owner");
        User firstMember = saveActiveUser("ws-first");
        User secondMember = saveActiveUser("ws-second");

        WorkspaceSummaryResponse workspaceSummary = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("members-workspace", "members description")
        );
        Workspace workspace = workspaceRepository.findById(workspaceSummary.workspaceId()).orElseThrow();

        LocalDateTime joinedAt = LocalDateTime.now();
        workspaceMemberRepository.save(WorkspaceMember.create(
                workspace,
                firstMember,
                WorkspaceRole.MEMBER,
                joinedAt.plusMinutes(1)
        ));
        workspaceMemberRepository.save(WorkspaceMember.create(
                workspace,
                secondMember,
                WorkspaceRole.MEMBER,
                joinedAt.plusMinutes(2)
        ));

        WorkspaceDetailResponse detail = workspaceService.getWorkspaceDetail(owner.getId(), workspace.getId());
        List<WorkspaceMemberResponse> members = workspaceService.getWorkspaceMembers(owner.getId(), workspace.getId());

        assertThat(detail.workspaceId()).isEqualTo(workspace.getId());
        assertThat(detail.myRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(detail.memberCount()).isEqualTo(3L);
        assertThat(members).hasSize(3);
        assertThat(members.get(0).userId()).isEqualTo(owner.getId());
        assertThat(members.get(1).userId()).isEqualTo(firstMember.getId());
        assertThat(members.get(2).userId()).isEqualTo(secondMember.getId());
    }

    @Test
    void updateWorkspaceTrimsFieldsAndRefreshesDetail() {
        User owner = saveActiveUser("ws-owner");
        WorkspaceSummaryResponse workspaceSummary = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("trim-workspace", "before")
        );
        Workspace workspaceBefore = workspaceRepository.findById(workspaceSummary.workspaceId()).orElseThrow();
        workspaceBefore.touch(LocalDateTime.now().minusMinutes(5));
        workspaceRepository.saveAndFlush(workspaceBefore);
        LocalDateTime updatedAtBefore = workspaceRepository.findById(workspaceSummary.workspaceId()).orElseThrow().getUpdatedAt();

        WorkspaceSummaryResponse updated = workspaceService.updateWorkspace(
                owner.getId(),
                workspaceSummary.workspaceId(),
                new UpdateWorkspaceRequest("trimmed-workspace", "  trimmed description  ")
        );
        WorkspaceDetailResponse detail = workspaceService.getWorkspaceDetail(owner.getId(), workspaceSummary.workspaceId());

        assertThat(updated.name()).isEqualTo("trimmed-workspace");
        assertThat(updated.description()).isEqualTo("trimmed description");
        assertThat(detail.name()).isEqualTo("trimmed-workspace");
        assertThat(detail.description()).isEqualTo("trimmed description");
        assertThat(detail.updatedAt()).isAfter(updatedAtBefore);
    }

    @Test
    void getWorkspaceProjectsReturnsAccessibleProjectsInUpdatedOrder() {
        User owner = saveActiveUser("ws-owner");
        User memberOnly = saveActiveUser("ws-member");

        WorkspaceSummaryResponse workspaceSummary = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("projects-workspace", "projects description")
        );
        Workspace workspace = workspaceRepository.findById(workspaceSummary.workspaceId()).orElseThrow();
        workspaceMemberRepository.save(WorkspaceMember.create(
                workspace,
                memberOnly,
                WorkspaceRole.MEMBER,
                LocalDateTime.now()
        ));

        projectService.createProject(
                owner.getId(),
                new CreateProjectRequest(workspaceSummary.workspaceId(), "alpha-project", "alpha")
        );
        projectService.createProject(
                owner.getId(),
                new CreateProjectRequest(workspaceSummary.workspaceId(), "beta-project", "beta")
        );

        List<ProjectListItemResponse> ownerProjects =
                workspaceService.getWorkspaceProjects(owner.getId(), workspaceSummary.workspaceId());
        List<ProjectListItemResponse> memberOnlyProjects =
                workspaceService.getWorkspaceProjects(memberOnly.getId(), workspaceSummary.workspaceId());

        assertThat(ownerProjects).hasSize(2);
        assertThat(ownerProjects.get(0).name()).isEqualTo("beta-project");
        assertThat(ownerProjects.get(1).name()).isEqualTo("alpha-project");
        assertThat(ownerProjects.get(0).role()).isEqualTo(com.taskflow.backend.global.common.enums.ProjectRole.OWNER);
        assertThat(ownerProjects.get(0).memberCount()).isEqualTo(1L);
        assertThat(memberOnlyProjects).isEmpty();
    }

    private User saveActiveUser(String nicknamePrefix) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname(normalizeNickname(nicknamePrefix))
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String normalizeNickname(String nicknamePrefix) {
        return nicknamePrefix.length() <= 20
                ? nicknamePrefix
                : nicknamePrefix.substring(0, 20);
    }
}
