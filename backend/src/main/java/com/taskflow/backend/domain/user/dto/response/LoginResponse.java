package com.taskflow.backend.domain.user.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        AuthUserResponse user
) {
}

