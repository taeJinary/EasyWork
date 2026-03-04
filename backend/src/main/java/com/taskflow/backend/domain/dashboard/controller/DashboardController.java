package com.taskflow.backend.domain.dashboard.controller;

import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectStatsResponse;
import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectsResponse;
import com.taskflow.backend.domain.dashboard.service.DashboardService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/projects")
    public ResponseEntity<ApiResponse<DashboardProjectsResponse>> getDashboardProjects(Authentication authentication) {
        DashboardProjectsResponse response = dashboardService.getDashboardProjects(extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/projects/{projectId}/dashboard")
    public ResponseEntity<ApiResponse<DashboardProjectStatsResponse>> getProjectDashboard(
            Authentication authentication,
            @PathVariable Long projectId
    ) {
        DashboardProjectStatsResponse response = dashboardService.getProjectDashboard(
                extractUserId(authentication),
                projectId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
