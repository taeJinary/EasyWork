package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.util.List;
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

    List<Task> findTaskBoardTasks(
            Long projectId,
            Long assigneeUserId,
            TaskPriority priority,
            Long labelId,
            String keyword
    );
}

