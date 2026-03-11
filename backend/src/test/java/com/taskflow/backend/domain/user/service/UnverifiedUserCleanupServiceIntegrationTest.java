package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.invitation.entity.WorkspaceInvitation;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.invitation.repository.WorkspaceInvitationRepository;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.EmailVerificationRetryJob;
import com.taskflow.backend.domain.user.entity.EmailVerificationToken;
import com.taskflow.backend.domain.user.entity.PasswordHistory;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationRetryJobRepository;
import com.taskflow.backend.domain.user.repository.EmailVerificationTokenRepository;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.infra.redis.RedisService;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UnverifiedUserCleanupServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private UnverifiedUserCleanupService unverifiedUserCleanupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private EmailVerificationRetryJobRepository emailVerificationRetryJobRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Autowired
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Autowired
    private ProjectInvitationRepository projectInvitationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        resetPersistentState();
    }

    @AfterEach
    void tearDown() {
        resetPersistentState();
    }

    @Test
    void cleanupDeletesExpiredUnverifiedLocalUserAndRelatedRows() {
        User owner = saveActiveUser("cleanup-owner");
        User candidate = saveUnverifiedLocalUser("cleanup-candidate");

        passwordHistoryRepository.save(PasswordHistory.create(candidate, "encoded-password"));
        emailVerificationTokenRepository.save(EmailVerificationToken.create(
                candidate,
                EmailVerificationTokenHashUtils.hash("cleanup-token-" + System.nanoTime()),
                LocalDateTime.now().plusHours(1)
        ));
        emailVerificationRetryJobRepository.save(EmailVerificationRetryJob.createPending(
                candidate.getId(),
                LocalDateTime.now(),
                "smtp down"
        ));
        notificationRepository.save(Notification.create(
                candidate,
                NotificationType.PROJECT_INVITED,
                "cleanup",
                "cleanup notification",
                NotificationReferenceType.INVITATION,
                1L
        ));
        notificationPushTokenRepository.save(NotificationPushToken.create(
                candidate,
                "cleanup-push-token-" + System.nanoTime(),
                PushPlatform.WEB
        ));

        Workspace workspace = workspaceRepository.save(Workspace.create(owner, "cleanup workspace", null));
        workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceRole.OWNER, LocalDateTime.now()));
        workspaceInvitationRepository.save(WorkspaceInvitation.create(
                workspace,
                owner,
                candidate,
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        ));

        Project project = projectRepository.save(Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name("cleanup project")
                .description(null)
                .build());
        projectMemberRepository.save(ProjectMember.create(project, owner, ProjectRole.OWNER, LocalDateTime.now()));
        projectInvitationRepository.save(ProjectInvitation.create(
                project,
                owner,
                candidate,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        ));

        ageUser(candidate.getId(), LocalDateTime.now().minusHours(25));
        redisService.setValue("refresh:" + candidate.getId() + ":device-1", "token", Duration.ofMinutes(30));
        redisService.setValue("email-verification:resend:cooldown:" + candidate.getId(), "1", Duration.ofMinutes(5));
        redisService.setValue("email-verification:resend:count:" + candidate.getId(), "2", Duration.ofMinutes(30));

        int deletedCount = unverifiedUserCleanupService.cleanupExpiredUnverifiedUsers(50);

        assertThat(deletedCount).isEqualTo(1);
        assertThat(userRepository.findById(candidate.getId())).isEmpty();
        assertThat(passwordHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(candidate.getId())).isEmpty();
        assertThat(emailVerificationTokenRepository.findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(candidate.getId()))
                .isEmpty();
        assertThat(emailVerificationRetryJobRepository.existsByUserIdAndCompletedAtIsNull(candidate.getId())).isFalse();
        assertThat(notificationRepository.findAllByUserIdAndIsReadFalse(candidate.getId())).isEmpty();
        assertThat(notificationPushTokenRepository.findAllByUserIdAndIsActiveTrue(candidate.getId())).isEmpty();
        assertThat(workspaceInvitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(candidate.getId())).isEmpty();
        assertThat(projectInvitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(candidate.getId())).isEmpty();
        assertThat(redisService.hasKey("refresh:" + candidate.getId() + ":device-1")).isFalse();
        assertThat(redisService.hasKey("email-verification:resend:cooldown:" + candidate.getId())).isFalse();
        assertThat(redisService.hasKey("email-verification:resend:count:" + candidate.getId())).isFalse();
    }

    @Test
    void cleanupSkipsExpiredUnverifiedUserWhenCollaborationMembershipExists() {
        User owner = saveActiveUser("cleanup-owner-membership");
        User candidate = saveUnverifiedLocalUser("cleanup-member");

        Workspace workspace = workspaceRepository.save(Workspace.create(owner, "membership workspace", null));
        workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceRole.OWNER, LocalDateTime.now()));
        workspaceMemberRepository.save(WorkspaceMember.create(workspace, candidate, WorkspaceRole.MEMBER, LocalDateTime.now()));

        ageUser(candidate.getId(), LocalDateTime.now().minusHours(25));
        emailVerificationTokenRepository.save(EmailVerificationToken.create(
                candidate,
                EmailVerificationTokenHashUtils.hash("membership-token-" + System.nanoTime()),
                LocalDateTime.now().plusHours(1)
        ));

        int deletedCount = unverifiedUserCleanupService.cleanupExpiredUnverifiedUsers(50);

        assertThat(deletedCount).isZero();
        assertThat(userRepository.findById(candidate.getId())).isPresent();
        assertThat(emailVerificationTokenRepository.findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(candidate.getId()))
                .hasSize(1);
    }

    @Test
    void cleanupContinuesPastSkippedCandidatesWithinSingleBatchRun() {
        User owner = saveActiveUser("cleanup-owner-paging");
        User skipped = saveUnverifiedLocalUser("cleanup-skipped");
        User deletable = saveUnverifiedLocalUser("cleanup-deletable");

        Workspace workspace = workspaceRepository.save(Workspace.create(owner, "paging workspace", null));
        workspaceMemberRepository.save(WorkspaceMember.create(workspace, owner, WorkspaceRole.OWNER, LocalDateTime.now()));
        workspaceMemberRepository.save(WorkspaceMember.create(workspace, skipped, WorkspaceRole.MEMBER, LocalDateTime.now()));

        ageUser(skipped.getId(), LocalDateTime.now().minusHours(25));
        ageUser(deletable.getId(), LocalDateTime.now().minusHours(25));

        int deletedCount = unverifiedUserCleanupService.cleanupExpiredUnverifiedUsers(1);

        assertThat(deletedCount).isEqualTo(1);
        assertThat(userRepository.findById(skipped.getId())).isPresent();
        assertThat(userRepository.findById(deletable.getId())).isEmpty();
    }

    private User saveActiveUser(String nicknamePrefix) {
        return userRepository.save(User.builder()
                .email(nicknamePrefix + "-" + System.nanoTime() + "@example.com")
                .password("encoded")
                .nickname(normalizeNickname(nicknamePrefix))
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(LocalDateTime.now())
                .build());
    }

    private User saveUnverifiedLocalUser(String nicknamePrefix) {
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

    private void ageUser(Long userId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "update users set created_at = ?, updated_at = ? where id = ?",
                createdAt,
                createdAt,
                userId
        );
    }

    private void resetPersistentState() {
        redisService.deleteByPattern("*");

        jdbcTemplate.execute("set foreign_key_checks = 0");
        List<String> tables = jdbcTemplate.queryForList("show tables", String.class);
        for (String table : tables) {
            if ("flyway_schema_history".equalsIgnoreCase(table)) {
                continue;
            }
            jdbcTemplate.execute("truncate table " + table);
        }
        jdbcTemplate.execute("set foreign_key_checks = 1");
    }
}
