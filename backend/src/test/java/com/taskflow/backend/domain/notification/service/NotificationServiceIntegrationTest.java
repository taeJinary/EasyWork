package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.comment.entity.Comment;
import com.taskflow.backend.domain.comment.repository.CommentRepository;
import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ProjectInvitationRepository projectInvitationRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private NotificationPushDispatchService notificationPushDispatchService;

    @MockBean
    private NotificationPushRetryService notificationPushRetryService;

    @BeforeEach
    void setUp() {
        given(notificationPushDispatchService.send(any(Notification.class)))
                .willReturn(new NotificationPushDispatchService.NotificationPushDispatchResult(Set.of()));
    }

    @Test
    void invitationNotificationPersistsAndReadFlowUpdatesUnreadCount() {
        User owner = saveActiveUser("owner");
        User invitee = saveActiveUser("invitee");
        Project project = saveProject(owner, "notification-project");
        ProjectInvitation invitation = projectInvitationRepository.save(ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        ));

        notificationService.createInvitationNotification(invitation);

        NotificationUnreadCountResponse unreadCount = notificationService.getUnreadCount(invitee.getId());
        NotificationListResponse notifications = notificationService.getNotifications(invitee.getId(), 0, 20, false);

        assertThat(unreadCount.unreadCount()).isEqualTo(1L);
        assertThat(notifications.content()).hasSize(1);
        assertThat(notifications.content().getFirst().type()).isEqualTo(NotificationType.PROJECT_INVITED);
        assertThat(notifications.content().getFirst().referenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(notifications.content().getFirst().referenceId()).isEqualTo(invitation.getId());
        assertThat(notifications.content().getFirst().isRead()).isFalse();

        Long notificationId = notifications.content().getFirst().notificationId();
        NotificationReadResponse readResponse = notificationService.readNotification(invitee.getId(), notificationId);

        assertThat(readResponse.notificationId()).isEqualTo(notificationId);
        assertThat(readResponse.isRead()).isTrue();
        assertThat(readResponse.readAt()).isNotNull();
        assertThat(notificationService.getUnreadCount(invitee.getId()).unreadCount()).isZero();
        assertThat(notificationRepository.findById(notificationId).orElseThrow().isRead()).isTrue();
    }

    @Test
    void readAllNotificationsMarksOnlyUnreadNotifications() {
        User user = saveActiveUser("reader");

        notificationRepository.save(Notification.create(
                user,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "first unread",
                NotificationReferenceType.INVITATION,
                10L
        ));
        notificationRepository.save(Notification.create(
                user,
                NotificationType.COMMENT_CREATED,
                "Comment added",
                "second unread",
                NotificationReferenceType.COMMENT,
                20L
        ));
        Notification alreadyRead = Notification.create(
                user,
                NotificationType.TASK_ASSIGNED,
                "Task assigned",
                "already read",
                NotificationReferenceType.TASK,
                30L
        );
        alreadyRead.markAsRead(LocalDateTime.now());
        notificationRepository.save(alreadyRead);

        NotificationReadAllResponse response = notificationService.readAllNotifications(user.getId());
        NotificationListResponse unreadOnly = notificationService.getNotifications(user.getId(), 0, 20, true);

        assertThat(response.updatedCount()).isEqualTo(2);
        assertThat(unreadOnly.content()).isEmpty();
        assertThat(notificationService.getUnreadCount(user.getId()).unreadCount()).isZero();
        assertThat(notificationRepository.findAll()).allMatch(Notification::isRead);
    }

    @Test
    void commentMentionAndCommentCreatedNotificationsSplitRecipientsWithoutDuplicates() {
        User creator = saveActiveUser("creator");
        User assignee = saveActiveUser("assignee");
        User actor = saveActiveUser("actor");
        Project project = saveProject(creator, "comment-project");
        saveMembership(project, creator, ProjectRole.OWNER);
        saveMembership(project, assignee, ProjectRole.MEMBER);
        saveMembership(project, actor, ProjectRole.MEMBER);

        Task task = taskRepository.save(Task.create(
                project,
                creator,
                assignee,
                "check-notification",
                "notification integration task",
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(1),
                0
        ));
        Comment comment = commentRepository.save(Comment.create(
                task,
                actor,
                "@assignee please review this @assignee"
        ));

        Set<Long> mentionedUserIds = notificationService.createCommentMentionNotifications(comment, actor);
        notificationService.createCommentCreatedNotification(comment, actor, mentionedUserIds);

        NotificationListResponse assigneeNotifications =
                notificationService.getNotifications(assignee.getId(), 0, 20, false);
        NotificationListResponse creatorNotifications =
                notificationService.getNotifications(creator.getId(), 0, 20, false);
        NotificationListResponse actorNotifications =
                notificationService.getNotifications(actor.getId(), 0, 20, false);

        assertThat(mentionedUserIds).containsExactly(assignee.getId());
        assertThat(assigneeNotifications.content()).hasSize(1);
        assertThat(assigneeNotifications.content().getFirst().type()).isEqualTo(NotificationType.COMMENT_MENTIONED);
        assertThat(creatorNotifications.content()).hasSize(1);
        assertThat(creatorNotifications.content().getFirst().type()).isEqualTo(NotificationType.COMMENT_CREATED);
        assertThat(actorNotifications.content()).isEmpty();
    }

    private Project saveProject(User owner, String projectName) {
        Workspace workspace = workspaceRepository.save(Workspace.create(
                owner,
                projectName + "-workspace",
                "notification integration workspace"
        ));
        return projectRepository.save(Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name(projectName)
                .description("notification integration project")
                .build());
    }

    private void saveMembership(Project project, User user, ProjectRole role) {
        projectMemberRepository.save(ProjectMember.create(project, user, role, LocalDateTime.now()));
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
