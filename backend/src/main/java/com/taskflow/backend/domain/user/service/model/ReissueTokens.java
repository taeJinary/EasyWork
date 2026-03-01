package com.taskflow.backend.domain.user.service.model;

public record ReissueTokens(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}

