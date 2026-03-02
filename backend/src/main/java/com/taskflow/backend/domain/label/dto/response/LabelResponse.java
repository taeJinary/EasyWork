package com.taskflow.backend.domain.label.dto.response;

public record LabelResponse(
        Long labelId,
        String name,
        String colorHex
) {
}
