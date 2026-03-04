package com.taskflow.backend.domain.user.service.model;

import com.taskflow.backend.domain.user.dto.response.AuthUserResponse;

public record LoginTokens(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        AuthUserResponse user
) {
}

