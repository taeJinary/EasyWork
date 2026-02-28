package com.taskflow.backend.domain.user.dto.response;

public record ReissueResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn
) {
}

