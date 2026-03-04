package com.taskflow.backend.domain.project.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.service.InvitationService;
import com.taskflow.backend.domain.project.dto.request.ChangeMemberRoleRequest;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectMemberResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectMemberManagementIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private InvitationService invitationService;

    @Test
    void ownerCanPromoteAndDemoteProjectMember() {
        User owner = saveActiveUser("member-flow-owner");
        User member = saveActiveUser("member-flow-member");
        ProjectSummaryResponse project = createProject(owner.getId(), "member-flow-project");

        Long memberId = inviteAndAccept(owner, member, project.projectId());

        ProjectMemberResponse promoted = projectService.changeMemberRole(
                owner.getId(),
                project.projectId(),
                memberId,
                new ChangeMemberRoleRequest(ProjectRole.OWNER)
        );
        ProjectMemberResponse demoted = projectService.changeMemberRole(
                owner.getId(),
                project.projectId(),
                memberId,
                new ChangeMemberRoleRequest(ProjectRole.MEMBER)
        );

        assertThat(promoted.role()).isEqualTo(ProjectRole.OWNER);
        assertThat(demoted.role()).isEqualTo(ProjectRole.MEMBER);

        List<ProjectMemberResponse> members = projectService.getProjectMembers(owner.getId(), project.projectId());
        assertThat(findMemberByUserId(members, owner.getId()).role()).isEqualTo(ProjectRole.OWNER);
        assertThat(findMemberByUserId(members, member.getId()).role()).isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    void memberManagementEnforcesOwnerPermissionAndLastOwnerConstraint() {
        User owner = saveActiveUser("member-guard-owner");
        User member = saveActiveUser("member-guard-member");
        ProjectSummaryResponse project = createProject(owner.getId(), "member-guard-project");
        inviteAndAccept(owner, member, project.projectId());
        List<ProjectMemberResponse> membersAfterAccept = projectService.getProjectMembers(owner.getId(), project.projectId());

        ProjectMemberResponse ownerMembership = findMemberByUserId(membersAfterAccept, owner.getId());
        ProjectMemberResponse memberMembership = findMemberByUserId(membersAfterAccept, member.getId());

        assertThatThrownBy(() -> projectService.changeMemberRole(
                member.getId(),
                project.projectId(),
                memberMembership.memberId(),
                new ChangeMemberRoleRequest(ProjectRole.OWNER)
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);

        assertThatThrownBy(() -> projectService.removeMember(
                member.getId(),
                project.projectId(),
                ownerMembership.memberId()
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONLY_OWNER_ALLOWED);

        assertThatThrownBy(() -> projectService.removeMember(
                owner.getId(),
                project.projectId(),
                ownerMembership.memberId()
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_REMOVE_LAST_OWNER);

        projectService.removeMember(owner.getId(), project.projectId(), memberMembership.memberId());

        List<ProjectMemberResponse> remainingMembers = projectService.getProjectMembers(owner.getId(), project.projectId());
        assertThat(remainingMembers).hasSize(1);
        assertThat(remainingMembers.get(0).userId()).isEqualTo(owner.getId());
        assertThat(remainingMembers.get(0).role()).isEqualTo(ProjectRole.OWNER);
    }

    private ProjectSummaryResponse createProject(Long ownerUserId, String projectName) {
        WorkspaceSummaryResponse workspace = workspaceService.createWorkspace(
                ownerUserId,
                new CreateWorkspaceRequest(projectName + "-workspace", "project member integration")
        );
        return projectService.createProject(
                ownerUserId,
                new CreateProjectRequest(workspace.workspaceId(), projectName, "project member integration")
        );
    }

    private Long inviteAndAccept(User owner, User invitee, Long projectId) {
        InvitationSummaryResponse invitation = invitationService.createInvitation(
                owner.getId(),
                projectId,
                new CreateInvitationRequest(invitee.getEmail(), ProjectRole.MEMBER)
        );
        invitationService.acceptInvitation(invitee.getId(), invitation.invitationId());
        List<ProjectMemberResponse> members = projectService.getProjectMembers(owner.getId(), projectId);
        return findMemberByUserId(members, invitee.getId()).memberId();
    }

    private ProjectMemberResponse findMemberByUserId(List<ProjectMemberResponse> members, Long userId) {
        return members.stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    private User saveActiveUser(String nicknamePrefix) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname(nicknamePrefix)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
