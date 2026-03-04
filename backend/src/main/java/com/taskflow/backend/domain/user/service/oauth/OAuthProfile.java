package com.taskflow.backend.domain.user.service.oauth;

public record OAuthProfile(
        String providerId,
        String email,
        String nickname
) {
}

