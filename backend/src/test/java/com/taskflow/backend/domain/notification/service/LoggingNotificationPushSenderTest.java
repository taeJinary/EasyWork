package com.taskflow.backend.domain.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingNotificationPushSenderTest {

    private final LoggingNotificationPushSender loggingNotificationPushSender = new LoggingNotificationPushSender();

    @Test
    void tokenHashReturnsMaskedPrefixWithoutRawToken() {
        String rawToken = "raw-token-value-123";

        String hash = loggingNotificationPushSender.tokenHash(rawToken);

        assertThat(hash).isNotBlank();
        assertThat(hash).doesNotContain(rawToken);
    }

    @Test
    void tokenHashReturnsEmptyWhenTokenIsBlank() {
        assertThat(loggingNotificationPushSender.tokenHash(" ")).isEqualTo("empty");
    }
}
