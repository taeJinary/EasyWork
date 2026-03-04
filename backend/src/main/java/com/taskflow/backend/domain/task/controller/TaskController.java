package com.taskflow.backend.domain.task.controller;

import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.request.MoveTaskRequest;
import com.taskflow.backend.domain.task.dto.request.UpdateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
import com.taskflow.backend.domain.task.dto.response.TaskMoveResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import jakarta.validation.Valid;
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

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskListResponse>> getTasks(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String keyword
    ) {
        TaskListResponse response = taskService.getTasks(
                extractUserId(authentication),
                projectId,
                page,
                size,
                status,
                sortBy,
                direction,
                keyword
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskDetailResponse>> getTaskDetail(
            Authentication authentication,
            @PathVariable Long taskId
    ) {
        TaskDetailResponse response = taskService.getTaskDetail(
                extractUserId(authentication),
                taskId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskDetailResponse>> updateTask(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskDetailResponse response = taskService.updateTask(
                extractUserId(authentication),
                taskId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response, "태스크가 수정되었습니다."));
    }

    @PatchMapping("/tasks/{taskId}/move")
    public ResponseEntity<ApiResponse<TaskMoveResponse>> moveTask(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskRequest request
    ) {
        TaskMoveResponse response = taskService.moveTask(
                extractUserId(authentication),
                taskId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response, "태스크 위치가 변경되었습니다."));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            Authentication authentication,
            @PathVariable Long taskId
    ) {
        taskService.deleteTask(
                extractUserId(authentication),
                taskId
        );
        return ResponseEntity.ok(ApiResponse.success(null, "태스크가 삭제되었습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
