package com.taskflow.backend.domain.user.dto.response;

import com.taskflow.backend.domain.user.entity.User;

public record AuthUserResponse(
        Long userId,
        String email,
        String nickname,
        String profileImg,
        String role
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImg(),
                user.getRole().name()
        );
    }
}

