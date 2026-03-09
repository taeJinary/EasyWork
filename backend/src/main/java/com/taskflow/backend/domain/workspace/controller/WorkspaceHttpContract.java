package com.taskflow.backend.domain.workspace.controller;

public final class WorkspaceHttpContract {

    public static final String BASE_PATH = "/workspaces";
    public static final String DETAIL_PATH = "/{workspaceId}";
    public static final String MEMBERS_PATH = "/{workspaceId}/members";
    public static final String PROJECTS_PATH = "/{workspaceId}/projects";

    private WorkspaceHttpContract() {
    }

    public static String detailPath(Long workspaceId) {
        return BASE_PATH + "/" + workspaceId;
    }

    public static String membersPath(Long workspaceId) {
        return detailPath(workspaceId) + "/members";
    }

    public static String projectsPath(Long workspaceId) {
        return detailPath(workspaceId) + "/projects";
    }
}
