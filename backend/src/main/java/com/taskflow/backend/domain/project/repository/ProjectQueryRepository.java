package com.taskflow.backend.domain.project.repository;

import com.taskflow.backend.global.common.enums.ProjectRole;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectQueryRepository {

    Page<ProjectListQueryResult> findMyProjects(
            Long userId,
            String keyword,
            ProjectRole role,
            Pageable pageable
    );

    record ProjectListQueryResult(
            Long projectId,
            String name,
            String description,
            ProjectRole role,
            Long memberCount,
            Long taskCount,
            Long doneTaskCount,
            LocalDateTime updatedAt
    ) {
    }
}
