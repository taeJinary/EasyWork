package com.taskflow.backend.domain.notification.controller;

import com.taskflow.backend.domain.notification.dto.request.RegisterNotificationPushTokenRequest;
import com.taskflow.backend.domain.notification.dto.response.NotificationPushTokenResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationPushTokenUnregisterResponse;
import com.taskflow.backend.domain.notification.service.NotificationPushTokenService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.security.ApiRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(NotificationPushTokenHttpContract.BASE_PATH)
@RequiredArgsConstructor
public class NotificationPushTokenController {

    private final NotificationPushTokenService notificationPushTokenService;
    private final ApiRateLimitService apiRateLimitService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationPushTokenResponse>> registerPushToken(
            HttpServletRequest httpServletRequest,
            Authentication authentication,
            @Valid @RequestBody RegisterNotificationPushTokenRequest request
    ) {
        Long userId = extractUserId(authentication);
        apiRateLimitService.checkPushTokenRegister(httpServletRequest, userId);
        NotificationPushTokenResponse response = notificationPushTokenService.registerPushToken(
                userId,
                request.token(),
                request.platform()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationPushTokenResponse>>> getActivePushTokens(
            Authentication authentication
    ) {
        List<NotificationPushTokenResponse> response =
                notificationPushTokenService.getActivePushTokens(extractUserId(authentication));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<NotificationPushTokenUnregisterResponse>> unregisterPushToken(
            Authentication authentication,
            @RequestParam String token
    ) {
        boolean removed = notificationPushTokenService.unregisterPushToken(extractUserId(authentication), token);
        return ResponseEntity.ok(ApiResponse.success(new NotificationPushTokenUnregisterResponse(removed)));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
