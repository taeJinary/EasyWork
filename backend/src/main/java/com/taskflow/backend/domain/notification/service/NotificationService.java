package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.invitation.entity.WorkspaceInvitation;
import com.taskflow.backend.domain.comment.entity.Comment;
import com.taskflow.backend.domain.notification.dto.response.NotificationCreatedEventPayload;
import com.taskflow.backend.domain.notification.dto.response.NotificationListItemResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.websocket.dto.WebSocketEventMessage;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_SIZE = 20;
    private static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";
    private static final String NOTIFICATION_CREATED_EVENT_TYPE = "NOTIFICATION_CREATED";
    private static final Pattern MENTION_NICKNAME_PATTERN = Pattern.compile("(?<!\\S)@([\\p{L}\\p{N}._-]{1,20})");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationPushDispatchService notificationPushDispatchService;
    private final NotificationPushRetryService notificationPushRetryService;

    public NotificationListResponse getNotifications(Long userId, int page, int size, boolean unreadOnly) {
        findActiveUser(userId);

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? size : DEFAULT_SIZE;
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<Notification> notificationPage = unreadOnly
                ? notificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationListItemResponse> content = notificationPage.getContent().stream()
                .map(notification -> new NotificationListItemResponse(
                        notification.getId(),
                        notification.getType(),
                        notification.getTitle(),
                        notification.getContent(),
                        notification.getReferenceType(),
                        notification.getReferenceId(),
                        notification.isRead(),
                        notification.getCreatedAt()
                ))
                .toList();

        return new NotificationListResponse(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isFirst(),
                notificationPage.isLast()
        );
    }

    public NotificationUnreadCountResponse getUnreadCount(Long userId) {
        findActiveUser(userId);
        return new NotificationUnreadCountResponse(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public NotificationReadResponse readNotification(Long userId, Long notificationId) {
        findActiveUser(userId);

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRead()) {
            notification.markAsRead(LocalDateTime.now());
        }

        return new NotificationReadResponse(
                notification.getId(),
                notification.isRead(),
                notification.getReadAt()
        );
    }

    @Transactional
    public NotificationReadAllResponse readAllNotifications(Long userId) {
        findActiveUser(userId);

        List<Notification> unreadNotifications = notificationRepository.findAllByUserIdAndIsReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> notification.markAsRead(now));

        return new NotificationReadAllResponse(unreadNotifications.size());
    }

    @Transactional
    public void createInvitationNotification(ProjectInvitation invitation) {
        Notification notification = Notification.create(
                invitation.getInvitee(),
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                invitation.getInviter().getNickname() + " invited you to " + invitation.getProject().getName(),
                NotificationReferenceType.INVITATION,
                invitation.getId()
        );
        Notification saved = notificationRepository.save(notification);
        publishNotificationCreatedEvent(
                saved,
                invitation.getProject().getId(),
                invitation.getInviter()
        );
    }

    @Transactional
    public void createWorkspaceInvitationNotification(WorkspaceInvitation invitation) {
        Notification notification = Notification.create(
                invitation.getInvitee(),
                NotificationType.PROJECT_INVITED,
                "Workspace invitation",
                invitation.getInviter().getNickname() + " invited you to " + invitation.getWorkspace().getName(),
                NotificationReferenceType.WORKSPACE_INVITATION,
                invitation.getId()
        );
        Notification saved = notificationRepository.save(notification);
        publishNotificationCreatedEvent(
                saved,
                invitation.getWorkspace().getId(),
                invitation.getInviter()
        );
    }

    @Transactional
    public void createInvitationAcceptedNotification(ProjectInvitation invitation) {
        Notification notification = Notification.create(
                invitation.getInviter(),
                NotificationType.INVITATION_ACCEPTED,
                "Invitation accepted",
                invitation.getInvitee().getNickname() + " accepted invitation to " + invitation.getProject().getName(),
                NotificationReferenceType.INVITATION,
                invitation.getId()
        );
        Notification saved = notificationRepository.save(notification);
        publishNotificationCreatedEvent(
                saved,
                invitation.getProject().getId(),
                invitation.getInvitee()
        );
    }

    @Transactional
    public void createTaskAssignedNotification(Task task, User actor) {
        User assignee = task.getAssignee();
        if (assignee == null || assignee.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = Notification.create(
                assignee,
                NotificationType.TASK_ASSIGNED,
                "Task assigned",
                actor.getNickname() + " assigned task: " + task.getTitle(),
                NotificationReferenceType.TASK,
                task.getId()
        );
        Notification saved = notificationRepository.save(notification);
        publishNotificationCreatedEvent(saved, task.getProject().getId(), actor);
    }

    @Transactional
    public void createCommentCreatedNotification(Comment comment, User actor) {
        createCommentCreatedNotification(comment, actor, Set.of());
    }

    @Transactional
    public void createCommentCreatedNotification(Comment comment, User actor, Set<Long> excludedRecipientIds) {
        Task task = comment.getTask();
        Map<Long, User> recipients = new LinkedHashMap<>();

        addRecipient(recipients, task.getCreator(), actor, excludedRecipientIds);
        addRecipient(recipients, task.getAssignee(), actor, excludedRecipientIds);

        for (User recipient : recipients.values()) {
            Notification notification = Notification.create(
                    recipient,
                    NotificationType.COMMENT_CREATED,
                    "Comment added",
                    actor.getNickname() + " commented on task: " + task.getTitle(),
                    NotificationReferenceType.COMMENT,
                    comment.getId()
            );
            Notification saved = notificationRepository.save(notification);
            publishNotificationCreatedEvent(saved, task.getProject().getId(), actor);
        }
    }

    @Transactional
    public Set<Long> createCommentMentionNotifications(Comment comment, User actor) {
        Set<String> mentionedNicknames = extractMentionedNicknames(comment.getContent());
        if (mentionedNicknames.isEmpty()) {
            return Set.of();
        }

        Long projectId = comment.getTask().getProject().getId();
        Map<String, List<User>> membersByNickname = projectMemberRepository
                .findAllByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(ProjectMember::getUser)
                .filter(user -> !user.isDeleted())
                .collect(java.util.stream.Collectors.groupingBy(User::getNickname));

        Set<Long> notifiedUserIds = new LinkedHashSet<>();
        for (String nickname : mentionedNicknames) {
            List<User> members = membersByNickname.getOrDefault(nickname, List.of());
            for (User recipient : members) {
                if (recipient.getId().equals(actor.getId())) {
                    continue;
                }
                if (!notifiedUserIds.add(recipient.getId())) {
                    continue;
                }

                Notification notification = Notification.create(
                        recipient,
                        NotificationType.COMMENT_MENTIONED,
                        "Mentioned in comment",
                        actor.getNickname() + " mentioned you in task: " + comment.getTask().getTitle(),
                        NotificationReferenceType.COMMENT,
                        comment.getId()
                );
                Notification saved = notificationRepository.save(notification);
                publishNotificationCreatedEvent(saved, projectId, actor);
            }
        }
        return Set.copyOf(notifiedUserIds);
    }

    private void addRecipient(Map<Long, User> recipients, User candidate, User actor, Set<Long> excludedRecipientIds) {
        if (candidate == null || candidate.getId().equals(actor.getId())) {
            return;
        }
        if (excludedRecipientIds != null && excludedRecipientIds.contains(candidate.getId())) {
            return;
        }
        recipients.putIfAbsent(candidate.getId(), candidate);
    }

    private Set<String> extractMentionedNicknames(String content) {
        if (!StringUtils.hasText(content)) {
            return Set.of();
        }

        Set<String> mentionedNicknames = new LinkedHashSet<>();
        Matcher matcher = MENTION_NICKNAME_PATTERN.matcher(content);
        while (matcher.find()) {
            mentionedNicknames.add(matcher.group(1));
        }
        return Set.copyOf(mentionedNicknames);
    }

    private void publishNotificationCreatedEvent(Notification notification, Long projectId, User actor) {
        LocalDateTime occurredAt = notification.getCreatedAt() != null
                ? notification.getCreatedAt()
                : LocalDateTime.now();

        NotificationCreatedEventPayload payload = new NotificationCreatedEventPayload(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceType(),
                notification.getReferenceId()
        );

        WebSocketEventMessage<NotificationCreatedEventPayload> message = new WebSocketEventMessage<>(
                NOTIFICATION_CREATED_EVENT_TYPE,
                projectId,
                occurredAt,
                new WebSocketEventMessage.EventActor(actor.getId(), actor.getNickname()),
                payload
        );

        messagingTemplate.convertAndSendToUser(
                notification.getUser().getEmail(),
                USER_NOTIFICATION_DESTINATION,
                message
        );
        NotificationPushDispatchService.NotificationPushDispatchResult dispatchResult =
                notificationPushDispatchService.send(notification);
        if (dispatchResult == null || dispatchResult.transientFailedTokenIds().isEmpty()) {
            return;
        }

        for (Long pushTokenId : dispatchResult.transientFailedTokenIds()) {
            notificationPushRetryService.enqueueFailure(
                    notification,
                    pushTokenId,
                    "Transient push delivery failure"
            );
        }
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }
}
