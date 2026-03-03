package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.global.common.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskQueryRepository {

    Page<Task> findTasks(
            Long projectId,
            TaskStatus status,
            String sortBy,
            String direction,
            String keyword,
            Pageable pageable
    );
}

