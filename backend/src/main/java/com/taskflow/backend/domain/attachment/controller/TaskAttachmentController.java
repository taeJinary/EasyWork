package com.taskflow.backend.domain.attachment.controller;

import com.taskflow.backend.domain.attachment.dto.response.TaskAttachmentResponse;
import com.taskflow.backend.domain.attachment.service.TaskAttachmentService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TaskAttachmentController {

    private final TaskAttachmentService taskAttachmentService;

    @PostMapping(
            value = "/tasks/{taskId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<TaskAttachmentResponse>> uploadAttachment(
            Authentication authentication,
            @PathVariable Long taskId,
            @RequestPart("file") MultipartFile file
    ) {
        TaskAttachmentResponse response = taskAttachmentService.uploadAttachment(
                extractUserId(authentication),
                taskId,
                file
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "첨부파일이 업로드되었습니다."));
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<ApiResponse<List<TaskAttachmentResponse>>> getTaskAttachments(
            Authentication authentication,
            @PathVariable Long taskId
    ) {
        List<TaskAttachmentResponse> response = taskAttachmentService.getTaskAttachments(
                extractUserId(authentication),
                taskId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            Authentication authentication,
            @PathVariable Long attachmentId
    ) {
        taskAttachmentService.deleteAttachment(extractUserId(authentication), attachmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "첨부파일이 삭제되었습니다."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
