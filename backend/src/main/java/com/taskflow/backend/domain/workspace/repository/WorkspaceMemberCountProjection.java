package com.taskflow.backend.domain.workspace.repository;

public interface WorkspaceMemberCountProjection {

    Long getWorkspaceId();

    long getMemberCount();
}
