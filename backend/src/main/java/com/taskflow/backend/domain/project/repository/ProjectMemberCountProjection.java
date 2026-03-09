package com.taskflow.backend.domain.project.repository;

public interface ProjectMemberCountProjection {

    Long getProjectId();

    long getMemberCount();
}
