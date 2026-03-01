package com.taskflow.backend.domain.project.dto.request;

import com.taskflow.backend.global.common.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(
        @NotNull(message = "role is required")
        ProjectRole role
) {
}
