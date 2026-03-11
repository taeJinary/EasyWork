package com.taskflow.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationVerifyRequest(
        @NotBlank(message = "인증 토큰은 필수입니다.")
        String token
) {
}
