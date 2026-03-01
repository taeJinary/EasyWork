package com.taskflow.backend.domain.project.repository;

import com.taskflow.backend.domain.project.entity.ProjectMember;
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

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId);

    long countByProjectId(Long projectId);
}

