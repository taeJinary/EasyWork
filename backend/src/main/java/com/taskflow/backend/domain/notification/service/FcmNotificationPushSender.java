package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.common.enums.PushPlatform;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Set<String> PERMANENT_FAILURE_ERRORS = Set.of(
            "InvalidRegistration",
            "NotRegistered",
            "MismatchSenderId"
    );
    private static final Set<String> RETRYABLE_FAILURE_ERRORS = Set.of(
            "Unavailable",
            "InternalServerError",
            "DeviceMessageRateExceeded",
            "TopicsMessageRateExceeded",
            "QuotaExceeded"
    );

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
            throw new PushDeliveryNonRetryableException("FCM server key is not configured.");
        }
        if (!StringUtils.hasText(token)) {
            throw new PushDeliveryNonRetryableException("Push token must not be blank.");
        }

        try {
            FcmSendResponse response = restClient.post()
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
                    .body(FcmSendResponse.class);

            validateDeliveryResult(response);
        } catch (RestClientException exception) {
            throw new PushDeliveryRetryableException("Failed to send push notification via FCM.", exception);
        }
    }

    private void validateDeliveryResult(FcmSendResponse response) {
        if (response == null) {
            throw new PushDeliveryNonRetryableException("FCM returned an empty response body.");
        }

        int failureCount = response.failure() == null ? 0 : response.failure();
        if (failureCount <= 0) {
            return;
        }

        String error = response.results() == null
                ? null
                : response.results().stream()
                .map(FcmSendResult::error)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        if (StringUtils.hasText(error) && PERMANENT_FAILURE_ERRORS.contains(error)) {
            throw new PushTokenInvalidException("FCM permanent token failure: " + error);
        }

        if (StringUtils.hasText(error) && RETRYABLE_FAILURE_ERRORS.contains(error)) {
            throw new PushDeliveryRetryableException("FCM transient delivery failure: " + error);
        }

        throw new PushDeliveryNonRetryableException("FCM non-retryable delivery failure: " + (error == null ? "unknown" : error));
    }

    private record FcmSendResponse(
            Integer success,
            Integer failure,
            List<FcmSendResult> results
    ) {
    }

    private record FcmSendResult(
            String error
    ) {
    }
}
