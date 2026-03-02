package com.taskflow.backend.domain.label.controller;

import com.taskflow.backend.domain.label.dto.request.CreateLabelRequest;
import com.taskflow.backend.domain.label.dto.request.UpdateLabelRequest;
import com.taskflow.backend.domain.label.dto.response.LabelResponse;
import com.taskflow.backend.domain.label.service.LabelService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @PostMapping("/projects/{projectId}/labels")
    public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateLabelRequest request
    ) {
        LabelResponse response = labelService.createLabel(extractUserId(authentication), projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Label has been created."));
    }

    @GetMapping("/projects/{projectId}/labels")
    public ResponseEntity<ApiResponse<List<LabelResponse>>> getLabels(
            Authentication authentication,
            @PathVariable Long projectId
    ) {
        List<LabelResponse> response = labelService.getLabels(extractUserId(authentication), projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<LabelResponse>> updateLabel(
            Authentication authentication,
            @PathVariable Long labelId,
            @Valid @RequestBody UpdateLabelRequest request
    ) {
        LabelResponse response = labelService.updateLabel(extractUserId(authentication), labelId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Label has been updated."));
    }

    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(
            Authentication authentication,
            @PathVariable Long labelId
    ) {
        labelService.deleteLabel(extractUserId(authentication), labelId);
        return ResponseEntity.ok(ApiResponse.success(null, "Label has been deleted."));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}