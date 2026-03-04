package com.taskflow.backend.domain.invitation.dto.request;

import com.taskflow.backend.global.common.enums.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotNull(message = "역할은 필수입니다.")
        ProjectRole role
) {
}

