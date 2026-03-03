package com.taskflow.backend.domain.workspace.repository;

import com.taskflow.backend.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
