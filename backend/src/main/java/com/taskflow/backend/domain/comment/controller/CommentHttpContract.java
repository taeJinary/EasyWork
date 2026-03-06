package com.taskflow.backend.domain.comment.controller;

public final class CommentHttpContract {

    public static final String TASK_COMMENTS_PATH = "/tasks/{taskId}/comments";
    public static final String COMMENT_PATH = "/comments/{commentId}";

    private CommentHttpContract() {
    }

    public static String taskCommentsPath(Long taskId) {
        return "/tasks/" + taskId + "/comments";
    }

    public static String commentPath(Long commentId) {
        return "/comments/" + commentId;
    }
}
