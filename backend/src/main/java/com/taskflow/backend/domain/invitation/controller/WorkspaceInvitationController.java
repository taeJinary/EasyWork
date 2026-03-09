package com.taskflow.backend.domain.invitation.controller;

import com.taskflow.backend.domain.invitation.dto.request.CreateWorkspaceInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.service.WorkspaceInvitationService;
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
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService workspaceInvitationService;
    private final ApiRateLimitService apiRateLimitService;

    @PostMapping(WorkspaceInvitationHttpContract.WORKSPACE_INVITATIONS_PATH)
    public ResponseEntity<ApiResponse<WorkspaceInvitationSummaryResponse>> createInvitation(
            HttpServletRequest httpServletRequest,
            Authentication authentication,
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateWorkspaceInvitationRequest request
    ) {
        Long userId = extractUserId(authentication);
        apiRateLimitService.checkInvitationCreate(httpServletRequest, userId);
        WorkspaceInvitationSummaryResponse response =
                workspaceInvitationService.createInvitation(userId, workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "초대가 생성되었습니다."));
    }

    @GetMapping(WorkspaceInvitationHttpContract.MY_INVITATIONS_PATH)
    public ResponseEntity<ApiResponse<WorkspaceInvitationListResponse>> getMyInvitations(
            Authentication authentication,
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        WorkspaceInvitationListResponse response =
                workspaceInvitationService.getMyInvitations(extractUserId(authentication), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(WorkspaceInvitationHttpContract.ACCEPT_PATH)
    public ResponseEntity<ApiResponse<WorkspaceInvitationActionResponse>> acceptInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        WorkspaceInvitationActionResponse response =
                workspaceInvitationService.acceptInvitation(extractUserId(authentication), invitationId);
        return ResponseEntity.ok(ApiResponse.success(response, "초대를 수락했습니다."));
    }

    @PostMapping(WorkspaceInvitationHttpContract.REJECT_PATH)
    public ResponseEntity<ApiResponse<WorkspaceInvitationActionResponse>> rejectInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        WorkspaceInvitationActionResponse response =
                workspaceInvitationService.rejectInvitation(extractUserId(authentication), invitationId);
        return ResponseEntity.ok(ApiResponse.success(response, "초대를 거절했습니다."));
    }

    @PostMapping(WorkspaceInvitationHttpContract.CANCEL_PATH)
    public ResponseEntity<ApiResponse<WorkspaceInvitationActionResponse>> cancelInvitation(
            Authentication authentication,
            @PathVariable Long workspaceId,
            @PathVariable Long invitationId
    ) {
        WorkspaceInvitationActionResponse response =
                workspaceInvitationService.cancelInvitation(extractUserId(authentication), workspaceId, invitationId);
        return ResponseEntity.ok(ApiResponse.success(response, "초대를 취소했습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
