package com.taskflow.backend.domain.task.repository;

import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.global.common.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);

    long countByProjectIdAndDeletedAtIsNull(Long projectId);

    long countByProjectIdAndStatusAndDeletedAtIsNull(Long projectId, TaskStatus status);

    @Query("""
            select t.project.id as projectId, count(t.id) as taskCount
            from Task t
            where t.project.id in :projectIds
              and t.deletedAt is null
            group by t.project.id
            """)
    List<ProjectTaskCountProjection> countTasksByProjectIds(@Param("projectIds") Set<Long> projectIds);

    @Query("""
            select t.project.id as projectId, count(t.id) as taskCount
            from Task t
            where t.project.id in :projectIds
              and t.deletedAt is null
              and t.status = :status
            group by t.project.id
            """)
    List<ProjectTaskCountProjection> countTasksByProjectIdsAndStatus(
            @Param("projectIds") Set<Long> projectIds,
            @Param("status") TaskStatus status
    );

    default List<ProjectTaskCountProjection> countDoneTasksByProjectIds(Set<Long> projectIds) {
        return countTasksByProjectIdsAndStatus(projectIds, TaskStatus.DONE);
    }

    long countByProjectIdAndDeletedAtIsNullAndDueDateBeforeAndStatusNot(
            Long projectId,
            LocalDate dueDate,
            TaskStatus status
    );

    long countByProjectIdAndDeletedAtIsNullAndDueDateBetweenAndStatusNot(
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            TaskStatus status
    );

    List<Task> findAllByProjectIdAndDeletedAtIsNullOrderByStatusAscPositionAsc(Long projectId);

    List<Task> findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(Long projectId, TaskStatus status);
}
