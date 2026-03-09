package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateWorkspaceInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListResponse;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkspaceInvitationServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private WorkspaceInvitationService workspaceInvitationService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createListAndAcceptWorkspaceInvitationFlowWorks() {
        User owner = saveActiveUser("ws-invite-owner");
        User invitee = saveActiveUser("ws-invite-member");

        WorkspaceSummaryResponse summary = workspaceService.createWorkspace(
                owner.getId(),
                new CreateWorkspaceRequest("workspace-invite", "desc")
        );
        Workspace workspace = workspaceRepository.findById(summary.workspaceId()).orElseThrow();

        workspaceInvitationService.createInvitation(
                owner.getId(),
                workspace.getId(),
                new CreateWorkspaceInvitationRequest(invitee.getEmail(), WorkspaceRole.MEMBER)
        );

        WorkspaceInvitationListResponse pendingInvitations =
                workspaceInvitationService.getMyInvitations(invitee.getId(), InvitationStatus.PENDING, 0, 20);

        assertThat(pendingInvitations.content()).hasSize(1);
        Long invitationId = pendingInvitations.content().getFirst().invitationId();

        WorkspaceInvitationActionResponse accepted =
                workspaceInvitationService.acceptInvitation(invitee.getId(), invitationId);

        assertThat(accepted.workspaceId()).isEqualTo(workspace.getId());
        assertThat(accepted.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), invitee.getId()))
                .isPresent();
    }

    private User saveActiveUser(String nicknamePrefix) {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + suffix + "@example.com")
                .password("encoded")
                .nickname(normalizeNickname(nicknamePrefix + suffix))
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String normalizeNickname(String nickname) {
        return nickname.length() <= 20 ? nickname : nickname.substring(0, 20);
    }
}
