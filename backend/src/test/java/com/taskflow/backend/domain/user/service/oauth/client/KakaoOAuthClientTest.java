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

class KakaoOAuthClientTest {

    @Test
    void fetchProfileReturnsMappedProfile() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthClient client = new KakaoOAuthClient(builder, "https://kakao.test/userinfo");

        server.expect(requestTo("https://kakao.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 123456,
                          "kakao_account": {
                            "email": "kakao@example.com",
                            "profile": {
                              "nickname": "kakao-user"
                            }
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        OAuthProfile profile = client.fetchProfile("access-token");

        assertThat(profile.providerId()).isEqualTo("123456");
        assertThat(profile.email()).isEqualTo("kakao@example.com");
        assertThat(profile.nickname()).isEqualTo("kakao-user");
        server.verify();
    }

    @Test
    void fetchProfileThrowsWhenProfileIsInvalid() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthClient client = new KakaoOAuthClient(builder, "https://kakao.test/userinfo");

        server.expect(requestTo("https://kakao.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 123456,
                          "kakao_account": {
                            "profile": {
                              "nickname": "kakao-user"
                            }
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
