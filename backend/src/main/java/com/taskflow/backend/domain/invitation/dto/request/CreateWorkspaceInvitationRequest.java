package com.taskflow.backend.domain.invitation.dto.request;

import com.taskflow.backend.global.common.enums.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkspaceInvitationRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email,

        @NotNull(message = "워크스페이스 역할은 필수입니다.")
        WorkspaceRole role
) {
}
