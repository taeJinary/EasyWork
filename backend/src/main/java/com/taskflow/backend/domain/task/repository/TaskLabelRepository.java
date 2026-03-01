package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.TaskLabel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, Long> {

    @Query("""
            select tl
            from TaskLabel tl
            join fetch tl.label
            where tl.task.id in :taskIds
            """)
    List<TaskLabel> findAllByTaskIdInWithLabel(@Param("taskIds") List<Long> taskIds);

    void deleteAllByTaskId(Long taskId);
}
