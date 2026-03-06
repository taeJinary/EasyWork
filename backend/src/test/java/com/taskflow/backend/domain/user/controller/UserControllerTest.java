package com.taskflow.backend.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.user.dto.request.ChangePasswordRequest;
import com.taskflow.backend.domain.user.dto.request.UpdateProfileRequest;
import com.taskflow.backend.domain.user.dto.request.WithdrawRequest;
import com.taskflow.backend.domain.user.dto.response.UserProfileResponse;
import com.taskflow.backend.domain.user.service.UserService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken principalAuth() {
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "user@example.com",
                "encoded",
                "ROLE_USER",
                UserStatus.ACTIVE
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Test
    void getMyProfileReturnsProfilePayload() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1L,
                "user@example.com",
                "tester",
                "https://cdn.example.com/profile.png",
                "LOCAL",
                LocalDateTime.of(2026, 2, 28, 10, 0)
        );

        given(userService.getMyProfile(1L)).willReturn(response);

        mockMvc.perform(get(UserHttpContract.mePath()).principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("tester"))
                .andExpect(jsonPath("$.data.provider").value("LOCAL"));
    }

    @Test
    void updateMyProfileReturnsUpdatedPayload() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("길동");
        UserProfileResponse response = new UserProfileResponse(
                1L,
                "user@example.com",
                "길동",
                "https://cdn.example.com/profile.png",
                "LOCAL",
                LocalDateTime.of(2026, 2, 28, 10, 0)
        );

        given(userService.updateMyProfile(eq(1L), any(UpdateProfileRequest.class))).willReturn(response);

        mockMvc.perform(patch(UserHttpContract.mePath())
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("길동"));
    }

    @Test
    void changePasswordReturnsOk() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123!", "NewPass123!");

        mockMvc.perform(patch(UserHttpContract.mePasswordPath())
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));

        then(userService).should().changePassword(eq(1L), any(ChangePasswordRequest.class));
    }

    @Test
    void withdrawReturnsOk() throws Exception {
        WithdrawRequest request = new WithdrawRequest("OldPass123!");

        mockMvc.perform(delete(UserHttpContract.mePath())
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

        then(userService).should().withdraw(eq(1L), any(WithdrawRequest.class));
    }
}
