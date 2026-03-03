package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.common.enums.PushPlatform;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "app.notification.push.sender", havingValue = "fcm")
public class FcmNotificationPushSender implements NotificationPushSender {

    private static final String FCM_AUTH_PREFIX = "key=";

    private final RestClient restClient;
    private final String fcmEndpoint;
    private final String serverKey;

    public FcmNotificationPushSender(
            RestClient.Builder restClientBuilder,
            @Value("${app.notification.push.fcm.endpoint:https://fcm.googleapis.com/fcm/send}") String fcmEndpoint,
            @Value("${app.notification.push.fcm.server-key:}") String serverKey
    ) {
        this.restClient = restClientBuilder.build();
        this.fcmEndpoint = fcmEndpoint;
        this.serverKey = serverKey;
    }

    @Override
    public void send(String token, PushPlatform platform, String title, String body) {
        if (!StringUtils.hasText(serverKey)) {
            throw new IllegalStateException("FCM server key is not configured.");
        }
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Push token must not be blank.");
        }

        try {
            restClient.post()
                    .uri(fcmEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, FCM_AUTH_PREFIX + serverKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "to", token,
                            "notification", Map.of(
                                    "title", title == null ? "" : title,
                                    "body", body == null ? "" : body
                            ),
                            "data", Map.of(
                                    "platform", platform.name()
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to send push notification via FCM.", exception);
        }
    }
}
