package com.taskflow.backend.domain.user.dto.request;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthCodeLoginRequest(
        @NotNull(message = "OAuth provider is required.")
        OAuthProvider provider,

        @NotBlank(message = "OAuth authorization code is required.")
        String authorizationCode,

        String codeVerifier,

        @NotBlank(message = "OAuth state is required.")
        String state
) {
}
