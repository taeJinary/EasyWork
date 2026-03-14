package com.taskflow.backend.global.config;

import com.taskflow.backend.domain.user.controller.AuthHttpContract;
import com.taskflow.backend.global.auth.jwt.JwtProperties;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.websocket.WebSocketContract;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void loginEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oauthLoginEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + "/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauthCodeLoginEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.OAUTH_CODE_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oauthAuthorizeUrlEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.OAUTH_AUTHORIZE_URL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oauthCodeLoginWithNaverRequiresState() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.OAUTH_CODE_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "NAVER",
                                  "authorizationCode": "valid-auth-code"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void tokenReissueEndpointPermitsAnonymousRequestAndReturnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.TOKEN_REISSUE_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void logoutEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.LOGOUT_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersMeEndpointReturns401ForAnonymousRequest() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectsEndpointReturns401ForAnonymousRequest() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invitationsEndpointReturns401ForAnonymousRequest() throws Exception {
        mockMvc.perform(get("/invitations/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfoEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void wsHandshakeEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1" + WebSocketContract.STOMP_ENDPOINT_PATH).contextPath("/api/v1"))
                .andExpect(anyOf(status().isBadRequest(), status().isSwitchingProtocols()));
    }

    @Test
    void protectedEndpointReturnsTokenInvalidWhenMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));
    }

    @Test
    void protectedEndpointReturnsTokenExpiredWhenExpiredBearerToken() throws Exception {
        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer " + expiredAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOKEN_EXPIRED"));
    }

    private String expiredAccessToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(jwtProperties.getSecret());
        properties.setAccessTokenExpiration(-1L);
        properties.setRefreshTokenExpiration(jwtProperties.getRefreshTokenExpiration());

        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(properties);
        return expiredTokenProvider.generateAccessToken(1L, "expired@example.com", Role.ROLE_USER);
    }

    private ResultMatcher anyOf(ResultMatcher... matchers) {
        return result -> {
            AssertionError last = null;
            for (ResultMatcher matcher : matchers) {
                try {
                    matcher.match(result);
                    return;
                } catch (AssertionError error) {
                    last = error;
                }
            }
            throw last != null ? last : new AssertionError("No matcher provided");
        };
    }
}
