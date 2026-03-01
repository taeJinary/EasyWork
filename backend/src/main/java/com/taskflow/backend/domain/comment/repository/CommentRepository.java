package com.taskflow.backend.domain.comment.repository;

import com.taskflow.backend.domain.comment.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdOrderByIdDesc(Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdAndIdLessThanOrderByIdDesc(Long taskId, Long id, Pageable pageable);
}
