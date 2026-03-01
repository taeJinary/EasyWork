package com.taskflow.backend.domain.task.dto.request;

import com.taskflow.backend.global.common.enums.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(
        @NotNull(message = "이동 대상 상태는 필수입니다.")
        TaskStatus toStatus,

        @NotNull(message = "targetPosition은 필수입니다.")
        @Min(value = 0, message = "targetPosition은 0 이상이어야 합니다.")
        Integer targetPosition,

        @NotNull(message = "version은 필수입니다.")
        Long version
) {
}
