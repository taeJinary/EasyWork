package com.taskflow.backend.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthCodeLoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthLoginRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.AuthUserResponse;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.service.AuthService;
import com.taskflow.backend.domain.user.service.model.LoginTokens;
import com.taskflow.backend.domain.user.service.model.ReissueTokens;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.security.ApiRateLimitService;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private ApiRateLimitService apiRateLimitService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void signupReturnsCreatedResponse() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "Pass123!", "tester");
        SignupResponse response = new SignupResponse(1L, "user@example.com", "tester");

        given(authService.signup(any(SignupRequest.class))).willReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    void signupReturnsBadRequestWhenValidationFails() throws Exception {
        SignupRequest request = new SignupRequest("invalid", "short", "a");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void loginReturnsTokenPayload() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Pass123!");
        LoginTokens tokens = new LoginTokens(
                "access-token",
                "refresh-token",
                1800000L,
                1209600000L,
                new AuthUserResponse(1L, "user@example.com", "tester", null, "ROLE_USER")
        );

        given(authService.login(any(LoginRequest.class))).willReturn(tokens);

                mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=1209600")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800000L))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.user.userId").value(1L));

        then(apiRateLimitService).should().checkAuthLogin(any(), eq("user@example.com"));
    }

    @Test
    void reissueReturnsNewTokens() throws Exception {
        ReissueTokens tokens = new ReissueTokens("new-access", "new-refresh", 1800000L, 1209600000L);

        given(authService.reissue(eq("refresh-token"))).willReturn(tokens);

        mockMvc.perform(post("/auth/token/reissue")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=1209600")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800000L))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

        then(apiRateLimitService).should().checkAuthTokenReissue(any());
    }

    @Test
    void reissueReturnsUnauthorizedWhenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/auth/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void oauthLoginReturnsTokenPayload() throws Exception {
        OAuthLoginRequest request = new OAuthLoginRequest(OAuthProvider.GOOGLE, "oauth-access-token");
        LoginTokens tokens = new LoginTokens(
                "oauth-access-token",
                "oauth-refresh-token",
                1800000L,
                1209600000L,
                new AuthUserResponse(2L, "oauth@example.com", "oauth-user", null, "ROLE_USER")
        );

        given(authService.oauthLogin(eq(request))).willReturn(tokens);

        mockMvc.perform(post("/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=1209600")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("oauth-access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800000L))
                .andExpect(jsonPath("$.data.user.userId").value(2L));

        then(apiRateLimitService).should().checkAuthOauthLogin(any());
    }

    @Test
    void oauthCodeLoginReturnsTokenPayload() throws Exception {
        OAuthCodeLoginRequest request = new OAuthCodeLoginRequest(OAuthProvider.GOOGLE, "auth-code", null, null);
        LoginTokens tokens = new LoginTokens(
                "oauth-access-token",
                "oauth-refresh-token",
                1800000L,
                1209600000L,
                new AuthUserResponse(2L, "oauth@example.com", "oauth-user", null, "ROLE_USER")
        );

        given(authService.oauthCodeLogin(eq(request))).willReturn(tokens);

        mockMvc.perform(post("/auth/oauth/code/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=1209600")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("oauth-access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800000L))
                .andExpect(jsonPath("$.data.user.userId").value(2L));

        then(apiRateLimitService).should().checkAuthOauthCodeLogin(any());
    }

    @Test
    void loginReturnsTooManyRequestsWhenRateLimited() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Pass123!");
        doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .when(apiRateLimitService)
                .checkAuthLogin(any(), eq("user@example.com"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"));

        then(authService).should(never()).login(any(LoginRequest.class));
    }

    @Test
    void logoutReturnsOkResponse() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));

        then(authService).should().logout(eq("access-token"), eq("refresh-token"));
    }

    @Test
    void logoutReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        then(authService).should(never()).logout(any(), any());
    }

    @Test
    void logoutReturnsUnauthorizedWhenAuthorizationHeaderIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Basic access-token")
                        .cookie(new Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        then(authService).should(never()).logout(any(), any());
    }
}
