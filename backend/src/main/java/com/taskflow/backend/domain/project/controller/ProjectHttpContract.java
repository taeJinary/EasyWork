package com.taskflow.backend.domain.project.controller;

public final class ProjectHttpContract {

    public static final String BASE_PATH = "/projects";
    public static final String DETAIL_PATH = "/{projectId}";
    public static final String MEMBERS_PATH = "/{projectId}/members";
    public static final String MEMBER_ROLE_PATH = "/{projectId}/members/{memberId}/role";
    public static final String MEMBER_PATH = "/{projectId}/members/{memberId}";

    private ProjectHttpContract() {
    }

    public static String detailPath(Long projectId) {
        return BASE_PATH + "/" + projectId;
    }

    public static String membersPath(Long projectId) {
        return detailPath(projectId) + "/members";
    }

    public static String memberRolePath(Long projectId, Long memberId) {
        return detailPath(projectId) + "/members/" + memberId + "/role";
    }

    public static String memberPath(Long projectId, Long memberId) {
        return detailPath(projectId) + "/members/" + memberId;
    }
}
