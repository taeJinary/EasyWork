package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.notification.dto.response.NotificationListItemResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.entity.Notification;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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
        notificationRepository.save(notification);
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
