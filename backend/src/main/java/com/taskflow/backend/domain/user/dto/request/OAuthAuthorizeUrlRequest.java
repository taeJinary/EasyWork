package com.taskflow.backend.domain.user.dto.request;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import jakarta.validation.constraints.NotNull;

public record OAuthAuthorizeUrlRequest(
        @NotNull(message = "OAuth provider is required.")
        OAuthProvider provider
) {
}
