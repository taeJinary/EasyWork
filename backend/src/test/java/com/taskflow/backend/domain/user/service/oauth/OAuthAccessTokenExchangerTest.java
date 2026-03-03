package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OAuthAccessTokenExchangerTest {

    @Test
    void exchangeReturnsAccessTokenForGoogleAuthorizationCode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OAuthAccessTokenExchanger exchanger = createExchanger(builder);

        server.expect(requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=authorization_code")))
                .andExpect(content().string(containsString("code=google-auth-code")))
                .andExpect(content().string(containsString("client_id=google-client-id")))
                .andExpect(content().string(containsString("client_secret=google-client-secret")))
                .andExpect(content().string(containsString("redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Foauth%2Fgoogle")))
                .andRespond(withSuccess(
                        """
                        {"access_token":"google-access-token"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        String token = exchanger.exchange(OAuthProvider.GOOGLE, "google-auth-code", null);

        assertThat(token).isEqualTo("google-access-token");
        server.verify();
    }

    @Test
    void exchangeThrowsWhenAccessTokenIsMissingInResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OAuthAccessTokenExchanger exchanger = createExchanger(builder);

        server.expect(requestTo("https://google.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"token_type":"Bearer"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> exchanger.exchange(OAuthProvider.GOOGLE, "google-auth-code", null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_TOKEN_INVALID);

        server.verify();
    }

    private OAuthAccessTokenExchanger createExchanger(RestClient.Builder builder) {
        return new OAuthAccessTokenExchanger(
                builder,
                "https://google.test/token",
                "google-client-id",
                "google-client-secret",
                "http://localhost:5173/oauth/google",
                "https://kakao.test/token",
                "kakao-client-id",
                "kakao-client-secret",
                "http://localhost:5173/oauth/kakao",
                "https://naver.test/token",
                "naver-client-id",
                "naver-client-secret",
                "http://localhost:5173/oauth/naver"
        );
    }
}
