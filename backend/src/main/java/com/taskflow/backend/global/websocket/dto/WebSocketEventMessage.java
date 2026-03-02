package com.taskflow.backend.global.websocket.dto;

import java.time.LocalDateTime;

public record WebSocketEventMessage<T>(
        String type,
        Long projectId,
        LocalDateTime occurredAt,
        EventActor actor,
        T payload
) {

    public record EventActor(
            Long userId,
            String nickname
    ) {
    }
}
