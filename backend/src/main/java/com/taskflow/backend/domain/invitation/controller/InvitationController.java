package com.taskflow.backend.domain.invitation.controller;

import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.service.InvitationService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.security.ApiRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final ApiRateLimitService apiRateLimitService;

    @PostMapping("/projects/{projectId}/invitations")
    public ResponseEntity<ApiResponse<InvitationSummaryResponse>> createInvitation(
            HttpServletRequest httpServletRequest,
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        Long userId = extractUserId(authentication);
        apiRateLimitService.checkInvitationCreate(httpServletRequest, userId);
        InvitationSummaryResponse response = invitationService.createInvitation(
                userId,
                projectId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "초대가 생성되었습니다."));
    }

    @GetMapping("/invitations/me")
    public ResponseEntity<ApiResponse<InvitationListResponse>> getMyInvitations(
            Authentication authentication,
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        InvitationListResponse response = invitationService.getMyInvitations(
                extractUserId(authentication),
                status,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<InvitationActionResponse>> acceptInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        InvitationActionResponse response = invitationService.acceptInvitation(
                extractUserId(authentication),
                invitationId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "초대를 수락했습니다."));
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<ApiResponse<InvitationActionResponse>> rejectInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        InvitationActionResponse response = invitationService.rejectInvitation(
                extractUserId(authentication),
                invitationId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "초대를 거절했습니다."));
    }

    @PostMapping("/projects/{projectId}/invitations/{invitationId}/cancel")
    public ResponseEntity<ApiResponse<InvitationActionResponse>> cancelInvitation(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long invitationId
    ) {
        InvitationActionResponse response = invitationService.cancelInvitation(
                extractUserId(authentication),
                projectId,
                invitationId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "초대를 취소했습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}

