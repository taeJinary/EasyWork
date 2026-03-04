package com.taskflow.backend.domain.comment.repository;

import com.taskflow.backend.domain.comment.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdOrderByIdDesc(Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdAndIdLessThanOrderByIdDesc(Long taskId, Long id, Pageable pageable);

    @Query("""
            select c.task.id as taskId, count(c.id) as commentCount
            from Comment c
            where c.task.id in :taskIds
            group by c.task.id
            """)
    List<TaskCommentCountProjection> countByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    long countByTaskId(Long taskId);

    interface TaskCommentCountProjection {
        Long getTaskId();
        Long getCommentCount();
    }
}
