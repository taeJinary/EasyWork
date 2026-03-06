package com.taskflow.backend.domain.notification.controller;

import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.service.NotificationService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(NotificationHttpContract.BASE_PATH)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        NotificationListResponse response = notificationService.getNotifications(
                extractUserId(authentication),
                page,
                size,
                unreadOnly
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(NotificationHttpContract.UNREAD_COUNT_PATH)
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            Authentication authentication
    ) {
        NotificationUnreadCountResponse response = notificationService.getUnreadCount(extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping(NotificationHttpContract.READ_PATH)
    public ResponseEntity<ApiResponse<NotificationReadResponse>> readNotification(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        NotificationReadResponse response = notificationService.readNotification(
                extractUserId(authentication),
                notificationId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(NotificationHttpContract.READ_ALL_PATH)
    public ResponseEntity<ApiResponse<NotificationReadAllResponse>> readAllNotifications(
            Authentication authentication
    ) {
        NotificationReadAllResponse response = notificationService.readAllNotifications(extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
