package com.taskflow.backend.domain.label.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(
        @NotBlank(message = "Label name is required.")
        @Size(max = 30, message = "Label name must be at most 30 characters.")
        String name,

        @NotBlank(message = "Label color is required.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid HEX code.")
        String colorHex
) {
}