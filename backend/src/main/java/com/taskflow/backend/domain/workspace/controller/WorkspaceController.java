package com.taskflow.backend.domain.workspace.controller;

import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectListItemResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceMemberResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping(WorkspaceHttpContract.BASE_PATH)
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceSummaryResponse>> createWorkspace(
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        WorkspaceSummaryResponse response = workspaceService.createWorkspace(extractUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Workspace created."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WorkspaceListResponse>> getMyWorkspaces(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        WorkspaceListResponse response = workspaceService.getMyWorkspaces(extractUserId(authentication), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(WorkspaceHttpContract.DETAIL_PATH)
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> getWorkspaceDetail(
            Authentication authentication,
            @PathVariable Long workspaceId
    ) {
        WorkspaceDetailResponse response =
                workspaceService.getWorkspaceDetail(extractUserId(authentication), workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(WorkspaceHttpContract.MEMBERS_PATH)
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> getWorkspaceMembers(
            Authentication authentication,
            @PathVariable Long workspaceId
    ) {
        List<WorkspaceMemberResponse> response =
                workspaceService.getWorkspaceMembers(extractUserId(authentication), workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(WorkspaceHttpContract.PROJECTS_PATH)
    public ResponseEntity<ApiResponse<List<ProjectListItemResponse>>> getWorkspaceProjects(
            Authentication authentication,
            @PathVariable Long workspaceId
    ) {
        List<ProjectListItemResponse> response =
                workspaceService.getWorkspaceProjects(extractUserId(authentication), workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping(WorkspaceHttpContract.DETAIL_PATH)
    public ResponseEntity<ApiResponse<WorkspaceSummaryResponse>> updateWorkspace(
            Authentication authentication,
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        WorkspaceSummaryResponse response =
                workspaceService.updateWorkspace(extractUserId(authentication), workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace updated."));
    }

    @DeleteMapping(WorkspaceHttpContract.DETAIL_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
            Authentication authentication,
            @PathVariable Long workspaceId
    ) {
        workspaceService.deleteWorkspace(extractUserId(authentication), workspaceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Workspace deleted."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
