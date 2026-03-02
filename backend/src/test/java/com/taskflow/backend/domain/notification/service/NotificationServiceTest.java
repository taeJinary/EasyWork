package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

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
                NotificationType.COMMENT_ADDED,
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

        notificationService.createInvitationNotification(invitation);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(2L);
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_INVITED);
        assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.INVITATION);
        assertThat(saved.getReferenceId()).isEqualTo(44L);
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
}
