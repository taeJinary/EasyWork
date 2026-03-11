package com.taskflow.backend.domain.project.repository;

import com.taskflow.backend.domain.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long projectId);

    List<Project> findAllByWorkspaceIdAndDeletedAtIsNull(Long workspaceId);

    boolean existsByOwnerIdAndDeletedAtIsNull(Long ownerId);
}

