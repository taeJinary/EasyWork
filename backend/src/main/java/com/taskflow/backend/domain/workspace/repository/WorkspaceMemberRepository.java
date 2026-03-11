package com.taskflow.backend.domain.workspace.repository;

import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findAllByUserIdOrderByWorkspaceUpdatedAtDesc(Long userId);

    boolean existsByUserId(Long userId);

    long countByWorkspaceId(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    List<WorkspaceMember> findAllByWorkspaceIdOrderByJoinedAtAsc(Long workspaceId);

    void deleteAllByWorkspaceId(Long workspaceId);

    @Query("""
            select wm
            from WorkspaceMember wm
            join fetch wm.user
            where wm.workspace.id = :workspaceId
            order by wm.joinedAt asc, wm.id asc
            """)
    List<WorkspaceMember> findAllWithUserByWorkspaceIdOrderByJoinedAtAsc(
            @Param("workspaceId") Long workspaceId
    );

    @Query(
            value = """
                    select wm
                    from WorkspaceMember wm
                    join fetch wm.workspace w
                    where wm.user.id = :userId
                    order by w.updatedAt desc
                    """,
            countQuery = """
                    select count(wm.id)
                    from WorkspaceMember wm
                    where wm.user.id = :userId
                    """
    )
    Page<WorkspaceMember> findByUserIdOrderByWorkspaceUpdatedAtDesc(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            select wm.workspace.id as workspaceId, count(wm.id) as memberCount
            from WorkspaceMember wm
            where wm.workspace.id in :workspaceIds
            group by wm.workspace.id
            """)
    List<WorkspaceMemberCountProjection> countMembersByWorkspaceIds(
            @Param("workspaceIds") Collection<Long> workspaceIds
    );
}
