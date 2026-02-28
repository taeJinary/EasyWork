package com.taskflow.backend.domain.user.dto.response;

import com.taskflow.backend.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long userId,
        String email,
        String nickname,
        String profileImg,
        String provider,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImg(),
                user.getProvider(),
                user.getCreatedAt()
        );
    }
}

