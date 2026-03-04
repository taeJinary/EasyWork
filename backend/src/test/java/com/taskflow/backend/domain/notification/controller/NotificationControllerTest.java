package com.taskflow.backend.domain.notification.controller;

import com.taskflow.backend.domain.notification.dto.response.NotificationListItemResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationListResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationReadResponse;
import com.taskflow.backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.taskflow.backend.domain.notification.service.NotificationService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.NotificationReferenceType;
import com.taskflow.backend.global.common.enums.NotificationType;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getNotificationsReturnsPagedResponse() throws Exception {
        NotificationListItemResponse item = new NotificationListItemResponse(
                300L,
                NotificationType.PROJECT_INVITED,
                "Project invitation",
                "owner invited you",
                NotificationReferenceType.INVITATION,
                10L,
                false,
                LocalDateTime.of(2026, 3, 2, 16, 0)
        );

        NotificationListResponse response = new NotificationListResponse(
                List.of(item),
                0,
                20,
                1,
                1,
                true,
                true
        );

        given(notificationService.getNotifications(1L, 0, 20, false)).willReturn(response);

        mockMvc.perform(get("/notifications")
                        .principal(principalAuth())
                        .param("page", "0")
                        .param("size", "20")
                        .param("unreadOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].notificationId").value(300L))
                .andExpect(jsonPath("$.data.content[0].type").value("PROJECT_INVITED"))
                .andExpect(jsonPath("$.data.page").value(0));

        then(notificationService).should().getNotifications(1L, 0, 20, false);
    }

    @Test
    void getUnreadCountReturnsResponse() throws Exception {
        NotificationUnreadCountResponse response = new NotificationUnreadCountResponse(3L);
        given(notificationService.getUnreadCount(1L)).willReturn(response);

        mockMvc.perform(get("/notifications/unread-count")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(3));

        then(notificationService).should().getUnreadCount(1L);
    }

    @Test
    void readNotificationReturnsResponse() throws Exception {
        NotificationReadResponse response = new NotificationReadResponse(
                300L,
                true,
                LocalDateTime.of(2026, 3, 2, 16, 5)
        );
        given(notificationService.readNotification(1L, 300L)).willReturn(response);

        mockMvc.perform(patch("/notifications/300/read")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationId").value(300L))
                .andExpect(jsonPath("$.data.isRead").value(true));

        then(notificationService).should().readNotification(1L, 300L);
    }

    @Test
    void readAllNotificationsReturnsResponse() throws Exception {
        NotificationReadAllResponse response = new NotificationReadAllResponse(5L);
        given(notificationService.readAllNotifications(1L)).willReturn(response);

        mockMvc.perform(post("/notifications/read-all")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(5));

        then(notificationService).should().readAllNotifications(1L);
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
