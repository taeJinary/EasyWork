package com.taskflow.backend.global.websocket;

import com.taskflow.backend.domain.comment.entity.Comment;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.websocket.dto.WebSocketEventMessage;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectBoardEventPublisher {

    private static final String BOARD_DESTINATION_FORMAT = "/topic/projects/%d/board";

    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_MOVED = "TASK_MOVED";
    public static final String TASK_DELETED = "TASK_DELETED";
    public static final String COMMENT_CREATED = "COMMENT_CREATED";
    public static final String COMMENT_DELETED = "COMMENT_DELETED";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishTaskCreated(Task task, User actor) {
        publish(task.getProject().getId(), TASK_CREATED, actor, taskPayload(task));
    }

    public void publishTaskUpdated(Task task, User actor) {
        publish(task.getProject().getId(), TASK_UPDATED, actor, taskPayload(task));
    }

    public void publishTaskMoved(Task task, User actor, Enum<?> fromStatus, Enum<?> toStatus) {
        Map<String, Object> payload = taskPayload(task);
        payload.put("fromStatus", fromStatus == null ? null : fromStatus.name());
        payload.put("toStatus", toStatus == null ? null : toStatus.name());
        publish(task.getProject().getId(), TASK_MOVED, actor, payload);
    }

    public void publishTaskDeleted(Task task, User actor) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        publish(task.getProject().getId(), TASK_DELETED, actor, payload);
    }

    public void publishCommentCreated(Comment comment, User actor, long commentCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commentId", comment.getId());
        payload.put("taskId", comment.getTask().getId());
        payload.put("commentCount", commentCount);
        publish(comment.getTask().getProject().getId(), COMMENT_CREATED, actor, payload);
    }

    public void publishCommentDeleted(Comment comment, User actor, long commentCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commentId", comment.getId());
        payload.put("taskId", comment.getTask().getId());
        payload.put("commentCount", commentCount);
        publish(comment.getTask().getProject().getId(), COMMENT_DELETED, actor, payload);
    }

    private Map<String, Object> taskPayload(Task task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("status", task.getStatus().name());
        payload.put("position", task.getPosition());
        return payload;
    }

    private void publish(Long projectId, String type, User actor, Map<String, Object> payload) {
        String destination = BOARD_DESTINATION_FORMAT.formatted(projectId);
        WebSocketEventMessage<Map<String, Object>> event = new WebSocketEventMessage<>(
                type,
                projectId,
                LocalDateTime.now(),
                new WebSocketEventMessage.EventActor(actor.getId(), actor.getNickname()),
                payload
        );
        messagingTemplate.convertAndSend(destination, event);
    }
}
