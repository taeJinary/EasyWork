package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.TaskStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<TaskStatusHistory> findTop10ByTaskIdOrderByCreatedAtDesc(Long taskId);
}
