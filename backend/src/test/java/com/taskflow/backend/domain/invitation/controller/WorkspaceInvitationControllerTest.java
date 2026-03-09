package com.taskflow.backend.domain.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.invitation.dto.request.CreateWorkspaceInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListItemResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.WorkspaceInvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.service.WorkspaceInvitationService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.global.security.ApiRateLimitService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceInvitationController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceInvitationService workspaceInvitationService;

    @MockBean
    private ApiRateLimitService apiRateLimitService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createWorkspaceInvitationReturnsCreatedResponse() throws Exception {
        CreateWorkspaceInvitationRequest request =
                new CreateWorkspaceInvitationRequest("member@example.com", WorkspaceRole.MEMBER);
        WorkspaceInvitationSummaryResponse response = new WorkspaceInvitationSummaryResponse(
                30L,
                10L,
                2L,
                "member@example.com",
                "member",
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.of(2026, 3, 10, 12, 0)
        );

        given(workspaceInvitationService.createInvitation(eq(1L), eq(10L), any(CreateWorkspaceInvitationRequest.class)))
                .willReturn(response);

        mockMvc.perform(post(WorkspaceInvitationHttpContract.workspaceInvitationsPath(10L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(10L))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        then(apiRateLimitService).should().checkInvitationCreate(any(), eq(1L));
    }

    @Test
    void getMyWorkspaceInvitationsReturnsPagedResponse() throws Exception {
        WorkspaceInvitationListItemResponse item = new WorkspaceInvitationListItemResponse(
                30L,
                10L,
                "TaskFlow Team",
                1L,
                "owner",
                WorkspaceRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.of(2026, 3, 10, 12, 0),
                LocalDateTime.of(2026, 3, 9, 12, 0)
        );
        WorkspaceInvitationListResponse response = new WorkspaceInvitationListResponse(
                List.of(item),
                0,
                20,
                1L,
                1,
                true,
                true
        );

        given(workspaceInvitationService.getMyInvitations(1L, InvitationStatus.PENDING, 0, 20))
                .willReturn(response);

        mockMvc.perform(get(WorkspaceInvitationHttpContract.MY_INVITATIONS_PATH)
                        .principal(principalAuth())
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].workspaceId").value(10L))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    void acceptWorkspaceInvitationReturnsOk() throws Exception {
        WorkspaceInvitationActionResponse response = new WorkspaceInvitationActionResponse(
                30L,
                10L,
                500L,
                WorkspaceRole.MEMBER,
                InvitationStatus.ACCEPTED
        );
        given(workspaceInvitationService.acceptInvitation(1L, 30L)).willReturn(response);

        mockMvc.perform(post(WorkspaceInvitationHttpContract.acceptPath(30L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.memberId").value(500L));
    }

    @Test
    void rejectWorkspaceInvitationReturnsOk() throws Exception {
        WorkspaceInvitationActionResponse response = new WorkspaceInvitationActionResponse(
                30L,
                10L,
                null,
                WorkspaceRole.MEMBER,
                InvitationStatus.REJECTED
        );
        given(workspaceInvitationService.rejectInvitation(1L, 30L)).willReturn(response);

        mockMvc.perform(post(WorkspaceInvitationHttpContract.rejectPath(30L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void cancelWorkspaceInvitationReturnsOk() throws Exception {
        WorkspaceInvitationActionResponse response = new WorkspaceInvitationActionResponse(
                30L,
                10L,
                null,
                WorkspaceRole.MEMBER,
                InvitationStatus.CANCELED
        );
        given(workspaceInvitationService.cancelInvitation(1L, 10L, 30L)).willReturn(response);

        mockMvc.perform(post(WorkspaceInvitationHttpContract.cancelPath(10L, 30L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    private UsernamePasswordAuthenticationToken principalAuth() {
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "owner@example.com",
                "encoded",
                "ROLE_USER",
                UserStatus.ACTIVE
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
