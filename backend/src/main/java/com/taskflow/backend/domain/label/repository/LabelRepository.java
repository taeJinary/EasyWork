package com.taskflow.backend.domain.label.repository;

import com.taskflow.backend.domain.label.entity.Label;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findAllByIdInAndProjectId(List<Long> ids, Long projectId);

    List<Label> findAllByProjectIdOrderByCreatedAtAsc(Long projectId);

    Optional<Label> findByIdAndProjectId(Long labelId, Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);

    boolean existsByProjectIdAndNameAndIdNot(Long projectId, String name, Long id);
}
