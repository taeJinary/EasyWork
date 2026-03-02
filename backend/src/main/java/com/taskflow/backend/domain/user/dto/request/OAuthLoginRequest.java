package com.taskflow.backend.domain.user.dto.request;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(
        @NotNull(message = "OAuth provider is required.")
        OAuthProvider provider,

        @NotBlank(message = "OAuth access token is required.")
        String accessToken
) {
}

