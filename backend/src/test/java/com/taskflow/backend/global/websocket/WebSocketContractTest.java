package com.taskflow.backend.global.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketContractTest {

    @Test
    void websocketPathsAndDestinationsRemainFrozen() {
        assertThat(WebSocketContract.STOMP_ENDPOINT_PATH).isEqualTo("/ws");
        assertThat(WebSocketContract.TOPIC_PREFIX).isEqualTo("/topic");
        assertThat(WebSocketContract.QUEUE_PREFIX).isEqualTo("/queue");
        assertThat(WebSocketContract.APPLICATION_PREFIX).isEqualTo("/app");
        assertThat(WebSocketContract.USER_PREFIX).isEqualTo("/user");
        assertThat(WebSocketContract.NOTIFICATION_QUEUE_DESTINATION)
                .isEqualTo("/user/queue/notifications");
        assertThat(WebSocketContract.projectBoardDestination(10L))
                .isEqualTo("/topic/projects/10/board");
    }
}
