package com.taskflow.backend.domain.dashboard.controller;

import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectStatsResponse;
import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectsResponse;
import com.taskflow.backend.domain.dashboard.service.DashboardService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.ProjectRole;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getDashboardProjectsReturnsResponse() throws Exception {
        DashboardProjectsResponse response = new DashboardProjectsResponse(
                2L,
                List.of(new DashboardProjectsResponse.MyProjectResponse(
                        10L,
                        "TaskFlow",
                        ProjectRole.OWNER,
                        3L,
                        12L,
                        4L,
                        33,
                        LocalDateTime.of(2026, 3, 2, 20, 0)
                ))
        );

        given(dashboardService.getDashboardProjects(1L)).willReturn(response);

        mockMvc.perform(get("/dashboard/projects")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingInvitationCount").value(2))
                .andExpect(jsonPath("$.data.myProjects[0].projectId").value(10L))
                .andExpect(jsonPath("$.data.myProjects[0].progressRate").value(33));

        then(dashboardService).should().getDashboardProjects(1L);
    }

    @Test
    void getProjectDashboardReturnsResponse() throws Exception {
        DashboardProjectStatsResponse response = new DashboardProjectStatsResponse(
                10L,
                3L,
                12L,
                4L,
                5L,
                3L,
                1L,
                2L,
                25
        );

        given(dashboardService.getProjectDashboard(1L, 10L)).willReturn(response);

        mockMvc.perform(get("/projects/10/dashboard")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.completionRate").value(25));

        then(dashboardService).should().getProjectDashboard(1L, 10L);
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
