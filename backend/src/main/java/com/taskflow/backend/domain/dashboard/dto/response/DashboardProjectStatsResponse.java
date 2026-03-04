package com.taskflow.backend.domain.dashboard.dto.response;

public record DashboardProjectStatsResponse(
        Long projectId,
        long memberCount,
        long taskCount,
        long todoCount,
        long inProgressCount,
        long doneCount,
        long overdueCount,
        long dueSoonCount,
        int completionRate
) {
}
