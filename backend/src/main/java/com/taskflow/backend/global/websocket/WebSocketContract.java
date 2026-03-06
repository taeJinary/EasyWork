package com.taskflow.backend.global.websocket;

import java.util.regex.Pattern;

public final class WebSocketContract {

    public static final String STOMP_ENDPOINT_PATH = "/ws";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String APPLICATION_PREFIX = "/app";
    public static final String USER_PREFIX = "/user";
    public static final String NOTIFICATION_QUEUE_DESTINATION = "/user/queue/notifications";

    private static final String PROJECT_BOARD_DESTINATION_FORMAT = "/topic/projects/%d/board";
    private static final Pattern PROJECT_BOARD_DESTINATION_PATTERN =
            Pattern.compile("^/topic/projects/(\\d+)/board$");

    private WebSocketContract() {
    }

    public static String projectBoardDestination(Long projectId) {
        return PROJECT_BOARD_DESTINATION_FORMAT.formatted(projectId);
    }

    public static Pattern projectBoardDestinationPattern() {
        return PROJECT_BOARD_DESTINATION_PATTERN;
    }
}
