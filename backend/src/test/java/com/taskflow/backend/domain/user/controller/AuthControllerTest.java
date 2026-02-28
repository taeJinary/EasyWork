package com.taskflow.backend.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.LogoutRequest;
import com.taskflow.backend.domain.user.dto.request.ReissueRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.AuthUserResponse;
import com.taskflow.backend.domain.user.dto.response.LoginResponse;
import com.taskflow.backend.domain.user.dto.response.ReissueResponse;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.service.AuthService;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void signupReturnsCreatedResponse() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "Pass123!", "tester");
        SignupResponse response = new SignupResponse(1L, "user@example.com", "tester");

        given(authService.signup(any(SignupRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/signup")
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

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void loginReturnsTokenPayload() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Pass123!");
        LoginResponse response = new LoginResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                1800000L,
                new AuthUserResponse(1L, "user@example.com", "tester", null, "ROLE_USER")
        );

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.userId").value(1L));
    }

    @Test
    void reissueReturnsNewTokens() throws Exception {
        ReissueRequest request = new ReissueRequest("refresh-token");
        ReissueResponse response = new ReissueResponse("new-access", "new-refresh", "Bearer", 1800000L);

        given(authService.reissue(any(ReissueRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void logoutReturnsOkResponse() throws Exception {
        LogoutRequest request = new LogoutRequest("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));

        then(authService).should().logout(eq("access-token"), any(LogoutRequest.class));
    }
}
