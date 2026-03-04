package com.taskflow.backend.domain.user.dto.response;

public record LoginResponse(
        String accessToken,
        long expiresIn,
        AuthUserResponse user
) {
}

