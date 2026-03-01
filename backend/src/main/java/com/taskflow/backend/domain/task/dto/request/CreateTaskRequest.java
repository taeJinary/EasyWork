package com.taskflow.backend.domain.task.dto.request;

import com.taskflow.backend.global.common.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateTaskRequest(
        @NotBlank(message = "태스크 제목은 필수입니다.")
        @Size(max = 100, message = "태스크 제목은 최대 100자입니다.")
        String title,

        String description,

        Long assigneeUserId,

        TaskPriority priority,

        LocalDate dueDate,

        List<Long> labelIds
) {
}
