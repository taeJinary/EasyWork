package com.taskflow.backend.domain.dashboard.controller;

public final class DashboardHttpContract {

    public static final String PROJECTS_PATH = "/dashboard/projects";
    public static final String PROJECT_DASHBOARD_PATH = "/projects/{projectId}/dashboard";

    private DashboardHttpContract() {
    }

    public static String projectDashboardPath(Long projectId) {
        return "/projects/" + projectId + "/dashboard";
    }
}
