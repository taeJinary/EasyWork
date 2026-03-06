package com.taskflow.backend.domain.attachment.controller;

public final class AttachmentHttpContract {

    public static final String TASK_ATTACHMENTS_PATH = "/tasks/{taskId}/attachments";
    public static final String ATTACHMENT_PATH = "/attachments/{attachmentId}";

    private AttachmentHttpContract() {
    }

    public static String taskAttachmentsPath(Long taskId) {
        return "/tasks/" + taskId + "/attachments";
    }

    public static String attachmentPath(Long attachmentId) {
        return "/attachments/" + attachmentId;
    }
}
