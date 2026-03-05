package com.taskflow.backend.domain.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationActionResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListItemResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.service.InvitationService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvitationService invitationService;

    @MockBean
    private ApiRateLimitService apiRateLimitService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken principalAuth() {
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "owner@example.com",
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
    void createInvitationReturnsCreatedResponse() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);
        InvitationSummaryResponse response = new InvitationSummaryResponse(
                10L,
                10L,
                2L,
                "member@example.com",
                "member",
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.of(2026, 3, 8, 10, 30)
        );

        given(invitationService.createInvitation(eq(1L), eq(10L), any(CreateInvitationRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/projects/10/invitations")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.inviteeEmail").value("member@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        then(apiRateLimitService).should().checkInvitationCreate(1L);
    }

    @Test
    void createInvitationReturnsTooManyRequestsWhenRateLimited() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest("member@example.com", ProjectRole.MEMBER);
        doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .when(apiRateLimitService)
                .checkInvitationCreate(1L);

        mockMvc.perform(post("/projects/10/invitations")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"));

        then(invitationService).should(never()).createInvitation(eq(1L), eq(10L), any(CreateInvitationRequest.class));
    }

    @Test
    void getMyInvitationsReturnsPagedResponse() throws Exception {
        InvitationListItemResponse item = new InvitationListItemResponse(
                10L,
                1L,
                "TaskFlow",
                1L,
                "owner",
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.of(2026, 3, 8, 10, 30),
                LocalDateTime.of(2026, 3, 1, 10, 30)
        );
        InvitationListResponse response = new InvitationListResponse(
                List.of(item),
                0,
                20,
                1L,
                1,
                true,
                true
        );

        given(invitationService.getMyInvitations(1L, InvitationStatus.PENDING, 0, 20)).willReturn(response);

        mockMvc.perform(get("/invitations/me?status=PENDING&page=0&size=20")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].invitationId").value(10L))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void acceptInvitationReturnsOk() throws Exception {
        InvitationActionResponse response = new InvitationActionResponse(
                10L,
                1L,
                500L,
                ProjectRole.MEMBER,
                InvitationStatus.ACCEPTED
        );
        given(invitationService.acceptInvitation(1L, 10L)).willReturn(response);

        mockMvc.perform(post("/invitations/10/accept")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invitationId").value(10L))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        then(invitationService).should().acceptInvitation(1L, 10L);
    }

    @Test
    void rejectInvitationReturnsOk() throws Exception {
        InvitationActionResponse response = new InvitationActionResponse(
                10L,
                1L,
                null,
                ProjectRole.MEMBER,
                InvitationStatus.REJECTED
        );
        given(invitationService.rejectInvitation(1L, 10L)).willReturn(response);

        mockMvc.perform(post("/invitations/10/reject")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invitationId").value(10L))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        then(invitationService).should().rejectInvitation(1L, 10L);
    }

    @Test
    void cancelInvitationReturnsOk() throws Exception {
        InvitationActionResponse response = new InvitationActionResponse(
                10L,
                10L,
                null,
                ProjectRole.MEMBER,
                InvitationStatus.CANCELED
        );
        given(invitationService.cancelInvitation(1L, 10L, 10L)).willReturn(response);

        mockMvc.perform(post("/projects/10/invitations/10/cancel")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        then(invitationService).should().cancelInvitation(1L, 10L, 10L);
    }
}
