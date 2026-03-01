package com.taskflow.backend.domain.label.repository;

import com.taskflow.backend.domain.label.entity.Label;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findAllByIdInAndProjectId(List<Long> ids, Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);
}
