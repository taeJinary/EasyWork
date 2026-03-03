package com.taskflow.backend.domain.user.service.oauth.client;

import com.taskflow.backend.domain.user.service.oauth.OAuthProfile;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverOAuthClientTest {

    @Test
    void fetchProfileReturnsMappedProfile() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverOAuthClient client = new NaverOAuthClient(builder, "https://naver.test/userinfo");

        server.expect(requestTo("https://naver.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "resultcode": "00",
                          "message": "success",
                          "response": {
                            "id": "naver-123",
                            "email": "naver@example.com",
                            "nickname": "naver-user",
                            "name": "Naver User"
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        OAuthProfile profile = client.fetchProfile("access-token");

        assertThat(profile.providerId()).isEqualTo("naver-123");
        assertThat(profile.email()).isEqualTo("naver@example.com");
        assertThat(profile.nickname()).isEqualTo("naver-user");
        server.verify();
    }

    @Test
    void fetchProfileThrowsWhenProfileIsInvalid() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverOAuthClient client = new NaverOAuthClient(builder, "https://naver.test/userinfo");

        server.expect(requestTo("https://naver.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "resultcode": "00",
                          "message": "success",
                          "response": {
                            "id": "naver-123"
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.fetchProfile("access-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_PROFILE_INVALID);

        server.verify();
    }
}
