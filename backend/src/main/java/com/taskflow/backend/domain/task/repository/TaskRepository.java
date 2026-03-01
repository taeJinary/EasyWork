package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);

    long countByProjectIdAndDeletedAtIsNull(Long projectId);

    long countByProjectIdAndStatusAndDeletedAtIsNull(Long projectId, TaskStatus status);

    List<Task> findAllByProjectIdAndDeletedAtIsNullOrderByStatusAscPositionAsc(Long projectId);

    List<Task> findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(Long projectId, TaskStatus status);
}
