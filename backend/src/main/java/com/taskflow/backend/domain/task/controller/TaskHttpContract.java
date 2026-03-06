package com.taskflow.backend.domain.task.controller;

public final class TaskHttpContract {

    public static final String PROJECT_TASKS_PATH = "/projects/{projectId}/tasks";
    public static final String PROJECT_TASK_BOARD_PATH = "/projects/{projectId}/tasks/board";
    public static final String TASK_DETAIL_PATH = "/tasks/{taskId}";
    public static final String TASK_MOVE_PATH = "/tasks/{taskId}/move";

    private TaskHttpContract() {
    }

    public static String projectTasksPath(Long projectId) {
        return "/projects/" + projectId + "/tasks";
    }

    public static String projectTaskBoardPath(Long projectId) {
        return projectTasksPath(projectId) + "/board";
    }

    public static String taskDetailPath(Long taskId) {
        return "/tasks/" + taskId;
    }

    public static String taskMovePath(Long taskId) {
        return taskDetailPath(taskId) + "/move";
    }
}
