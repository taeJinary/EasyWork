package com.taskflow.backend.domain.project.repository;

import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.global.common.enums.ProjectRole;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("""
            select pm
            from ProjectMember pm
            join fetch pm.project p
            where pm.user.id = :userId
              and p.deletedAt is null
            order by p.updatedAt desc
            """)
    List<ProjectMember> findAllActiveByUserIdOrderByProjectUpdatedAtDesc(@Param("userId") Long userId);

    @Query("""
            select pm
            from ProjectMember pm
            join fetch pm.project p
            where pm.user.id = :userId
              and p.workspace.id = :workspaceId
              and p.deletedAt is null
            order by p.updatedAt desc
            """)
    List<ProjectMember> findAllActiveByWorkspaceIdAndUserIdOrderByProjectUpdatedAtDesc(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );

    @Query("""
            select pm.project.id as projectId, count(pm.id) as memberCount
            from ProjectMember pm
            where pm.project.id in :projectIds
            group by pm.project.id
            """)
    List<ProjectMemberCountProjection> countMembersByProjectIds(@Param("projectIds") Set<Long> projectIds);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByIdAndProjectId(Long id, Long projectId);

    List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId);

    long countByProjectIdAndRole(Long projectId, ProjectRole role);

    long countByProjectId(Long projectId);
}

