package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.comment.entity.Comment;
import com.taskflow.backend.domain.notification.dto.response.NotificationCreatedEventPayload;
import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.websocket.dto.WebSocketEventMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationPushDispatchService notificationPushDispatchService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getNotificationsReturnsPage() {
        User member = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                member,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );
        ReflectionTestUtils.setField(notification, "id", 300L);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 3, 2, 16, 0));

        Page<Notification> page = new PageImpl<>(
                List.of(notification),
                PageRequest.of(0, 20),
                1
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(member));
        given(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .willReturn(page);

        NotificationListResponse response = notificationService.getNotifications(1L, 0, 20, false);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().notificationId()).isEqualTo(300L);
        assertThat(response.content().getFirst().referenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getUnreadCountReturnsCount() {
        User member = activeUser(1L, "member@example.com", "member");
        given(userRepository.findById(1L)).willReturn(Optional.of(member));
        given(notificationRepository.countByUserIdAndIsReadFalse(1L)).willReturn(3L);

        NotificationUnreadCountResponse response = notificationService.getUnreadCount(1L);

        assertThat(response.unreadCount()).isEqualTo(3L);
    }

    @Test
    void readNotificationMarksAsRead() {
        User member = activeUser(1L, "member@example.com", "member");
        Notification notification = Notification.create(
                member,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );
        ReflectionTestUtils.setField(notification, "id", 300L);

        given(userRepository.findById(1L)).willReturn(Optional.of(member));
        given(notificationRepository.findByIdAndUserId(300L, 1L)).willReturn(Optional.of(notification));

        NotificationReadResponse response = notificationService.readNotification(1L, 300L);

        assertThat(response.notificationId()).isEqualTo(300L);
        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void readNotificationThrowsWhenNotFound() {
        User member = activeUser(1L, "member@example.com", "member");
        given(userRepository.findById(1L)).willReturn(Optional.of(member));
        given(notificationRepository.findByIdAndUserId(999L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.readNotification(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void readAllNotificationsMarksUnread() {
        User member = activeUser(1L, "member@example.com", "member");
        Notification n1 = Notification.create(
                member,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L
        );
        Notification n2 = Notification.create(
                member,
                NotificationType.COMMENT_CREATED,
                "Comment added",
                "new comment",
                NotificationReferenceType.COMMENT,
                20L
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(member));
        given(notificationRepository.findAllByUserIdAndIsReadFalse(1L)).willReturn(List.of(n1, n2));

        NotificationReadAllResponse response = notificationService.readAllNotifications(1L);

        assertThat(response.updatedCount()).isEqualTo(2L);
        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        assertThat(n1.getReadAt()).isNotNull();
    }

    @Test
    void createInvitationNotificationSavesEntity() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "member");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );
        ReflectionTestUtils.setField(invitation, "id", 44L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 20, 30);
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 901L);
            ReflectionTestUtils.setField(notification, "createdAt", createdAt);
            return notification;
        });

        notificationService.createInvitationNotification(invitation);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(2L);
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_INVITED);
        assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(saved.getReferenceId()).isEqualTo(44L);

        ArgumentCaptor<WebSocketEventMessage> eventCaptor = ArgumentCaptor.forClass(WebSocketEventMessage.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("member@example.com"),
                eq("/queue/notifications"),
                eventCaptor.capture()
        );

        WebSocketEventMessage<?> eventMessage = eventCaptor.getValue();
        assertThat(eventMessage.type()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(eventMessage.projectId()).isEqualTo(10L);
        assertThat(eventMessage.occurredAt()).isEqualTo(createdAt);
        assertThat(eventMessage.actor().userId()).isEqualTo(1L);
        assertThat(eventMessage.actor().nickname()).isEqualTo("owner");

        NotificationCreatedEventPayload payload = (NotificationCreatedEventPayload) eventMessage.payload();
        assertThat(payload.notificationId()).isEqualTo(901L);
        assertThat(payload.type()).isEqualTo(NotificationType.PROJECT_INVITED);
        assertThat(payload.referenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(payload.referenceId()).isEqualTo(44L);
        verify(notificationPushDispatchService).send(any(Notification.class));
    }

    @Test
    void createInvitationAcceptedNotificationSavesEntityAndPublishesRealtimeEvent() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "member@example.com", "member");
        Project project = Project.builder()
                .id(10L)
                .owner(owner)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                owner,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.ACCEPTED,
                LocalDateTime.now().plusDays(7)
        );
        ReflectionTestUtils.setField(invitation, "id", 77L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 21, 15);
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 902L);
            ReflectionTestUtils.setField(notification, "createdAt", createdAt);
            return notification;
        });

        notificationService.createInvitationAcceptedNotification(invitation);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo(NotificationType.INVITATION_ACCEPTED);
        assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(saved.getReferenceId()).isEqualTo(77L);

        ArgumentCaptor<WebSocketEventMessage> eventCaptor = ArgumentCaptor.forClass(WebSocketEventMessage.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("owner@example.com"),
                eq("/queue/notifications"),
                eventCaptor.capture()
        );

        WebSocketEventMessage<?> eventMessage = eventCaptor.getValue();
        assertThat(eventMessage.type()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(eventMessage.projectId()).isEqualTo(10L);
        assertThat(eventMessage.occurredAt()).isEqualTo(createdAt);
        assertThat(eventMessage.actor().userId()).isEqualTo(2L);
        assertThat(eventMessage.actor().nickname()).isEqualTo("member");

        NotificationCreatedEventPayload payload = (NotificationCreatedEventPayload) eventMessage.payload();
        assertThat(payload.notificationId()).isEqualTo(902L);
        assertThat(payload.type()).isEqualTo(NotificationType.INVITATION_ACCEPTED);
        assertThat(payload.referenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(payload.referenceId()).isEqualTo(77L);
    }

    @Test
    void createTaskAssignedNotificationSavesEntityAndPublishesRealtimeEvent() {
        User actor = activeUser(1L, "owner@example.com", "owner");
        User assignee = activeUser(2L, "assignee@example.com", "assignee");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        Task task = Task.builder()
                .id(100L)
                .project(project)
                .creator(actor)
                .assignee(assignee)
                .title("Implement websocket")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .position(0)
                .version(0L)
                .build();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 22, 0);
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 903L);
            ReflectionTestUtils.setField(notification, "createdAt", createdAt);
            return notification;
        });

        notificationService.createTaskAssignedNotification(task, actor);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(2L);
        assertThat(saved.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.TASK);
        assertThat(saved.getReferenceId()).isEqualTo(100L);

        ArgumentCaptor<WebSocketEventMessage> eventCaptor = ArgumentCaptor.forClass(WebSocketEventMessage.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("assignee@example.com"),
                eq("/queue/notifications"),
                eventCaptor.capture()
        );
        WebSocketEventMessage<?> eventMessage = eventCaptor.getValue();
        assertThat(eventMessage.type()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(eventMessage.projectId()).isEqualTo(10L);
        assertThat(eventMessage.actor().userId()).isEqualTo(1L);

        NotificationCreatedEventPayload payload = (NotificationCreatedEventPayload) eventMessage.payload();
        assertThat(payload.type()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(payload.referenceType()).isEqualTo(NotificationReferenceType.TASK);
        assertThat(payload.referenceId()).isEqualTo(100L);
        verify(notificationPushDispatchService).send(any(Notification.class));
    }

    @Test
    void createTaskAssignedNotificationDoesNothingWhenAssigneeIsActor() {
        User actor = activeUser(1L, "owner@example.com", "owner");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        Task task = Task.builder()
                .id(100L)
                .project(project)
                .creator(actor)
                .assignee(actor)
                .title("Implement websocket")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .position(0)
                .version(0L)
                .build();

        notificationService.createTaskAssignedNotification(task, actor);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
        verify(notificationPushDispatchService, never()).send(any(Notification.class));
    }

    @Test
    void createCommentCreatedNotificationCreatesNotificationsForCreatorAndAssignee() {
        User creator = activeUser(1L, "creator@example.com", "creator");
        User assignee = activeUser(2L, "assignee@example.com", "assignee");
        User actor = activeUser(3L, "commenter@example.com", "commenter");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("desc")
                .build();
        Task task = Task.builder()
                .id(100L)
                .project(project)
                .creator(creator)
                .assignee(assignee)
                .title("Implement websocket")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .position(0)
                .version(0L)
                .build();
        Comment comment = Comment.create(task, actor, "Looks good");
        ReflectionTestUtils.setField(comment, "id", 555L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 22, 10);
        final long[] idSequence = {1000L};
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", idSequence[0]++);
            ReflectionTestUtils.setField(notification, "createdAt", createdAt);
            return notification;
        });

        notificationService.createCommentCreatedNotification(comment, actor);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertThat(savedNotifications).hasSize(2);
        assertThat(savedNotifications).allMatch(notification -> notification.getType() == NotificationType.COMMENT_CREATED);
        assertThat(savedNotifications).allMatch(notification -> notification.getReferenceType() == NotificationReferenceType.COMMENT);
        assertThat(savedNotifications).allMatch(notification -> notification.getReferenceId().equals(555L));
        assertThat(savedNotifications.stream().map(notification -> notification.getUser().getId()))
                .containsExactlyInAnyOrder(1L, 2L);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebSocketEventMessage> eventCaptor = ArgumentCaptor.forClass(WebSocketEventMessage.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                userCaptor.capture(),
                eq("/queue/notifications"),
                eventCaptor.capture()
        );
        assertThat(Set.copyOf(userCaptor.getAllValues()))
                .containsExactlyInAnyOrder("creator@example.com", "assignee@example.com");
        assertThat(eventCaptor.getAllValues()).allMatch(event -> event.type().equals("NOTIFICATION_CREATED"));
        verify(notificationPushDispatchService, times(2)).send(any(Notification.class));
    }

    @Test
    void createCommentCreatedNotificationExcludesMentionRecipients() {
        User creator = activeUser(1L, "creator@example.com", "creator");
        User assignee = activeUser(2L, "assignee@example.com", "assignee");
        User actor = activeUser(3L, "commenter@example.com", "commenter");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("desc")
                .build();
        Task task = Task.builder()
                .id(100L)
                .project(project)
                .creator(creator)
                .assignee(assignee)
                .title("Implement websocket")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .position(0)
                .version(0L)
                .build();
        Comment comment = Comment.create(task, actor, "Looks good");
        ReflectionTestUtils.setField(comment, "id", 556L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 22, 20);
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 1100L);
            ReflectionTestUtils.setField(notification, "createdAt", createdAt);
            return notification;
        });

        notificationService.createCommentCreatedNotification(comment, actor, Set.of(1L));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(2L);
        assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT_CREATED);
        verify(messagingTemplate).convertAndSendToUser(eq("assignee@example.com"), eq("/queue/notifications"), any());
        verify(notificationPushDispatchService).send(any(Notification.class));
    }

    @Test
    void createCommentMentionNotificationsCreatesNotificationsForMentionedMembers() {
        User creator = activeUser(1L, "creator@example.com", "creator");
        User actor = activeUser(2L, "actor@example.com", "actor");
        User mentioned = activeUser(3L, "mentioned@example.com", "alex");
        User other = activeUser(4L, "other@example.com", "other");
        Project project = Project.builder()
                .id(10L)
                .owner(creator)
                .name("TaskFlow")
                .description("desc")
                .build();
        Task task = Task.builder()
                .id(100L)
                .project(project)
                .creator(creator)
                .assignee(other)
                .title("Implement websocket")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .position(0)
                .version(0L)
                .build();
        Comment comment = Comment.create(task, actor, "Please check this @alex and @ghost, thanks @alex");
        ReflectionTestUtils.setField(comment, "id", 557L);

        ProjectMember actorMembership = member(201L, project, actor, ProjectRole.MEMBER);
        ProjectMember mentionedMembership = member(202L, project, mentioned, ProjectRole.MEMBER);
        ProjectMember otherMembership = member(203L, project, other, ProjectRole.MEMBER);

        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 22, 30);
        given(projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(actorMembership, mentionedMembership, otherMembership));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 1200L);
            ReflectionTestUtils.setField(notification, "createdAt", createdAt);
            return notification;
        });

        Set<Long> mentionedUserIds = notificationService.createCommentMentionNotifications(comment, actor);

        assertThat(mentionedUserIds).containsExactly(3L);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(3L);
        assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT_MENTIONED);
        assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.COMMENT);
        assertThat(saved.getReferenceId()).isEqualTo(557L);
        verify(messagingTemplate).convertAndSendToUser(eq("mentioned@example.com"), eq("/queue/notifications"), any());
        verify(notificationPushDispatchService).send(any(Notification.class));
    }

    @Test
    void getNotificationsThrowsWhenUserDeleted() {
        User deletedUser = User.builder()
                .id(1L)
                .email("deleted@example.com")
                .password("encoded")
                .nickname("deleted")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.DELETED)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> notificationService.getNotifications(1L, 0, 20, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
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

    private ProjectMember member(Long id, Project project, User user, ProjectRole role) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
    }
}
