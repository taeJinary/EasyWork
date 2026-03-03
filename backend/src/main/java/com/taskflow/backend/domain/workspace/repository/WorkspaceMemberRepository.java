package com.taskflow.backend.domain.workspace.repository;

import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findAllByUserIdOrderByWorkspaceUpdatedAtDesc(Long userId);

    long countByWorkspaceId(Long workspaceId);
}
