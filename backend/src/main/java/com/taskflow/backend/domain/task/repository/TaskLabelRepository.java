package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.TaskLabel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, Long> {
}
