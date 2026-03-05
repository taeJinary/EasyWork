package com.taskflow.backend.domain.notification.controller;

import com.taskflow.backend.domain.notification.dto.response.NotificationPushTokenResponse;
import com.taskflow.backend.domain.notification.service.NotificationPushTokenService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.PushPlatform;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.security.ApiRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationPushTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPushTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationPushTokenService notificationPushTokenService;

    @MockBean
    private ApiRateLimitService apiRateLimitService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerPushTokenReturnsResponse() throws Exception {
        NotificationPushTokenResponse response = new NotificationPushTokenResponse(
                "token-1",
                PushPlatform.WEB,
                true
        );
        given(notificationPushTokenService.registerPushToken(1L, "token-1", PushPlatform.WEB)).willReturn(response);

        mockMvc.perform(post("/notifications/push-tokens")
                        .principal(principalAuth())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-1",
                                  "platform": "WEB"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token-1"))
                .andExpect(jsonPath("$.data.platform").value("WEB"))
                .andExpect(jsonPath("$.data.active").value(true));

        then(apiRateLimitService).should().checkPushTokenRegister(1L);
        then(notificationPushTokenService).should().registerPushToken(1L, "token-1", PushPlatform.WEB);
    }

    @Test
    void registerPushTokenReturnsTooManyRequestsWhenRateLimited() throws Exception {
        doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .when(apiRateLimitService)
                .checkPushTokenRegister(1L);

        mockMvc.perform(post("/notifications/push-tokens")
                        .principal(principalAuth())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "token-1",
                                  "platform": "WEB"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"));

        then(notificationPushTokenService).should(never()).registerPushToken(1L, "token-1", PushPlatform.WEB);
    }

    @Test
    void unregisterPushTokenReturnsRemovedFlag() throws Exception {
        given(notificationPushTokenService.unregisterPushToken(1L, "token-1")).willReturn(true);

        mockMvc.perform(delete("/notifications/push-tokens")
                        .principal(principalAuth())
                        .param("token", "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.removed").value(true));

        then(notificationPushTokenService).should().unregisterPushToken(1L, "token-1");
    }

    private UsernamePasswordAuthenticationToken principalAuth() {
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "member@example.com",
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
}
