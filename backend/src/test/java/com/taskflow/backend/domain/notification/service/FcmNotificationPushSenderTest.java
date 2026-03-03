package com.taskflow.backend.domain.notification.service;

import com.taskflow.backend.global.common.enums.PushPlatform;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FcmNotificationPushSenderTest {

    @Test
    void sendCallsFcmWithExpectedHeadersAndPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FcmNotificationPushSender sender = new FcmNotificationPushSender(
                builder,
                "https://fcm.test/send",
                "fcm-server-key"
        );

        server.expect(requestTo("https://fcm.test/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "key=fcm-server-key"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().json(
                        """
                        {
                          "to": "token-value",
                          "notification": {
                            "title": "Notice",
                            "body": "Body text"
                          },
                          "data": {
                            "platform": "WEB"
                          }
                        }
                        """
                ))
                .andRespond(withSuccess("{\"success\":1}", MediaType.APPLICATION_JSON));

        sender.send("token-value", PushPlatform.WEB, "Notice", "Body text");

        server.verify();
    }

    @Test
    void sendThrowsWhenServerKeyMissing() {
        RestClient.Builder builder = RestClient.builder();
        FcmNotificationPushSender sender = new FcmNotificationPushSender(
                builder,
                "https://fcm.test/send",
                " "
        );

        assertThatThrownBy(() -> sender.send("token", PushPlatform.WEB, "Notice", "Body"))
                .isInstanceOf(PushDeliveryNonRetryableException.class);
    }

    @Test
    void sendThrowsInvalidTokenExceptionWhenFcmReturnsPermanentFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FcmNotificationPushSender sender = new FcmNotificationPushSender(
                builder,
                "https://fcm.test/send",
                "fcm-server-key"
        );

        server.expect(requestTo("https://fcm.test/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                          "success": 0,
                          "failure": 1,
                          "results": [
                            {"error":"NotRegistered"}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> sender.send("token-value", PushPlatform.WEB, "Notice", "Body"))
                .isInstanceOf(PushTokenInvalidException.class);

        server.verify();
    }

    @Test
    void sendThrowsRetryableExceptionWhenFcmReturnsTransientFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FcmNotificationPushSender sender = new FcmNotificationPushSender(
                builder,
                "https://fcm.test/send",
                "fcm-server-key"
        );

        server.expect(requestTo("https://fcm.test/send"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                          "success": 0,
                          "failure": 1,
                          "results": [
                            {"error":"Unavailable"}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> sender.send("token-value", PushPlatform.WEB, "Notice", "Body"))
                .isInstanceOf(PushDeliveryRetryableException.class);

        server.verify();
    }
}
