package com.taskflow.backend.domain.project.controller;

import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.request.ChangeMemberRoleRequest;
import com.taskflow.backend.domain.project.dto.request.UpdateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectDetailResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectMemberResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.service.ProjectService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.taskflow.backend.global.common.enums.ProjectRole;

@RestController
@RequestMapping(ProjectHttpContract.BASE_PATH)
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectSummaryResponse>> createProject(
            Authentication authentication,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectSummaryResponse response = projectService.createProject(extractUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "프로젝트가 생성되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> getMyProjects(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProjectRole role
    ) {
        ProjectListResponse response = projectService.getMyProjects(
                extractUserId(authentication),
                page,
                size,
                keyword,
                role
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(ProjectHttpContract.DETAIL_PATH)
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectDetail(
            Authentication authentication,
            @PathVariable Long projectId
    ) {
        ProjectDetailResponse response = projectService.getProjectDetail(extractUserId(authentication), projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping(ProjectHttpContract.DETAIL_PATH)
    public ResponseEntity<ApiResponse<ProjectSummaryResponse>> updateProject(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        ProjectSummaryResponse response = projectService.updateProject(extractUserId(authentication), projectId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "프로젝트가 수정되었습니다."));
    }

    @DeleteMapping(ProjectHttpContract.DETAIL_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            Authentication authentication,
            @PathVariable Long projectId
    ) {
        projectService.deleteProject(extractUserId(authentication), projectId);
        return ResponseEntity.ok(ApiResponse.success(null, "프로젝트가 삭제되었습니다."));
    }

    @GetMapping(ProjectHttpContract.MEMBERS_PATH)
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(
            Authentication authentication,
            @PathVariable Long projectId
    ) {
        List<ProjectMemberResponse> response = projectService.getProjectMembers(extractUserId(authentication), projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping(ProjectHttpContract.MEMBER_ROLE_PATH)
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> changeMemberRole(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeMemberRoleRequest request
    ) {
        ProjectMemberResponse response = projectService.changeMemberRole(
                extractUserId(authentication),
                projectId,
                memberId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response, "멤버 역할이 변경되었습니다."));
    }

    @DeleteMapping(ProjectHttpContract.MEMBER_PATH)
    public ResponseEntity<ApiResponse<Void>> removeMember(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long memberId
    ) {
        projectService.removeMember(extractUserId(authentication), projectId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "멤버가 제거되었습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}

