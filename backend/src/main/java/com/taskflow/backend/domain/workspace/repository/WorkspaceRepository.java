package com.taskflow.backend.domain.workspace.repository;

import com.taskflow.backend.domain.workspace.entity.Workspace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findByIdAndDeletedAtIsNull(Long workspaceId);

    boolean existsByOwnerIdAndDeletedAtIsNull(Long ownerId);
}
