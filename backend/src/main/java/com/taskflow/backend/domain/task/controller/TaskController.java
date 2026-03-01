package com.taskflow.backend.domain.task.controller;

import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.common.enums.TaskPriority;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskSummaryResponse>> createTask(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskSummaryResponse response = taskService.createTask(
                extractUserId(authentication),
                projectId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "태스크가 생성되었습니다."));
    }

    @GetMapping("/projects/{projectId}/tasks/board")
    public ResponseEntity<ApiResponse<TaskBoardResponse>> getTaskBoard(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(required = false) Long assigneeUserId,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) String keyword
    ) {
        TaskBoardResponse response = taskService.getTaskBoard(
                extractUserId(authentication),
                projectId,
                assigneeUserId,
                priority,
                labelId,
                keyword
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
