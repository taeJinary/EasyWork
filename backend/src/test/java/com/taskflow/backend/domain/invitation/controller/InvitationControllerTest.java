package com.taskflow.backend.domain.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.invitation.dto.request.CreateInvitationRequest;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListItemResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationListResponse;
import com.taskflow.backend.domain.invitation.dto.response.InvitationSummaryResponse;
import com.taskflow.backend.domain.invitation.service.InvitationService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.UserStatus;
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
                "팀원",
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
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("초대가 생성되었습니다."));
    }

    @Test
    void getMyInvitationsReturnsPagedResponse() throws Exception {
        InvitationListItemResponse item = new InvitationListItemResponse(
                10L,
                1L,
                "TaskFlow",
                1L,
                "오너",
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
}
