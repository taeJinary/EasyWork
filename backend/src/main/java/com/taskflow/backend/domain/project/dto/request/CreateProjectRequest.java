package com.taskflow.backend.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "프로젝트명은 필수입니다.")
        @Size(min = 2, max = 50, message = "프로젝트명은 2~50자여야 합니다.")
        String name,

        @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
        String description
) {
}

