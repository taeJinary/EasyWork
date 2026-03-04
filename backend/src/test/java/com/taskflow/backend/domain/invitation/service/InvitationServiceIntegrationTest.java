package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvitationServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectInvitationRepository projectInvitationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void invitationLifecycleCreatesMembershipAndNotifications() {
        User owner = saveActiveUser("owner");
        User invitee = saveActiveUser("invitee");
        Project project = saveProject(owner);
        projectMemberRepository.save(ProjectMember.create(project, owner, ProjectRole.OWNER, LocalDateTime.now()));

        InvitationSummaryResponse created = invitationService.createInvitation(
                owner.getId(),
                project.getId(),
                new CreateInvitationRequest(invitee.getEmail(), ProjectRole.MEMBER)
        );

        List<Notification> inviteeNotifications = notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(invitee.getId(), PageRequest.of(0, 20))
                .getContent();
        assertThat(inviteeNotifications)
                .extracting(Notification::getType)
                .contains(NotificationType.PROJECT_INVITED);

        InvitationListResponse invitationList = invitationService.getMyInvitations(
                invitee.getId(),
                InvitationStatus.PENDING,
                0,
                20
        );
        assertThat(invitationList.content()).hasSize(1);
        assertThat(invitationList.content().get(0).invitationId()).isEqualTo(created.invitationId());

        InvitationActionResponse accepted = invitationService.acceptInvitation(invitee.getId(), created.invitationId());

        assertThat(accepted.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), invitee.getId())).isPresent();

        ProjectInvitation savedInvitation = projectInvitationRepository.findById(created.invitationId()).orElseThrow();
        assertThat(savedInvitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);

        List<Notification> inviterNotifications = notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(owner.getId(), PageRequest.of(0, 20))
                .getContent();
        assertThat(inviterNotifications).anySatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.INVITATION_ACCEPTED);
            assertThat(notification.getReferenceId()).isEqualTo(created.invitationId());
        });
    }

    @Test
    void getMyInvitationsNormalizesExpiredPendingInvitationInDatabase() {
        User owner = saveActiveUser("owner-expired");
        User invitee = saveActiveUser("invitee-expired");
        Project project = saveProject(owner);
        projectMemberRepository.save(ProjectMember.create(project, owner, ProjectRole.OWNER, LocalDateTime.now()));

        ProjectInvitation expiredPending = projectInvitationRepository.save(ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().minusMinutes(10)
        ));

        InvitationListResponse response = invitationService.getMyInvitations(
                invitee.getId(),
                InvitationStatus.PENDING,
                0,
                20
        );

        assertThat(response.content()).isEmpty();
        ProjectInvitation reloaded = projectInvitationRepository.findById(expiredPending.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
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

    private Project saveProject(User owner) {
        Workspace workspace = workspaceRepository.save(Workspace.create(
                owner,
                "workspace-" + System.nanoTime(),
                "integration-test-workspace"
        ));

        return projectRepository.save(Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name("project-" + System.nanoTime())
                .description("integration-test-project")
                .build());
    }
}
