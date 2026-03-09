package com.taskflow.backend.domain.task.repository;

public interface ProjectTaskCountProjection {

    Long getProjectId();

    long getTaskCount();
}
