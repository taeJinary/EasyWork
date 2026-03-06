package com.taskflow.backend.domain.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListItemResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceMemberResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.service.WorkspaceService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createWorkspaceReturnsCreatedResponse() throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("TaskFlow Team", "team workspace");
        WorkspaceSummaryResponse response = new WorkspaceSummaryResponse(
                10L,
                "TaskFlow Team",
                "team workspace",
                WorkspaceRole.OWNER
        );
        given(workspaceService.createWorkspace(eq(1L), any(CreateWorkspaceRequest.class))).willReturn(response);

        mockMvc.perform(post(WorkspaceHttpContract.BASE_PATH)
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(10L))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"));
    }

    @Test
    void getMyWorkspacesReturnsPagedResponse() throws Exception {
        WorkspaceListItemResponse item = new WorkspaceListItemResponse(
                10L,
                "TaskFlow Team",
                "team workspace",
                WorkspaceRole.OWNER,
                1L,
                LocalDateTime.of(2026, 3, 3, 9, 0)
        );
        WorkspaceListResponse response = new WorkspaceListResponse(
                List.of(item),
                0,
                20,
                1L,
                1,
                true,
                true
        );
        given(workspaceService.getMyWorkspaces(1L, 0, 20)).willReturn(response);

        mockMvc.perform(get(WorkspaceHttpContract.BASE_PATH)
                        .principal(principalAuth())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].workspaceId").value(10L))
                .andExpect(jsonPath("$.data.content[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.totalElements").value(1L));

        then(workspaceService).should().getMyWorkspaces(1L, 0, 20);
    }

    @Test
    void getWorkspaceDetailReturnsResponse() throws Exception {
        WorkspaceDetailResponse response = new WorkspaceDetailResponse(
                10L,
                "TaskFlow Team",
                "team workspace",
                WorkspaceRole.OWNER,
                2L,
                LocalDateTime.of(2026, 3, 3, 9, 0)
        );
        given(workspaceService.getWorkspaceDetail(1L, 10L)).willReturn(response);

        mockMvc.perform(get(WorkspaceHttpContract.detailPath(10L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(10L))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(2L));
    }

    @Test
    void getWorkspaceMembersReturnsResponse() throws Exception {
        WorkspaceMemberResponse owner = new WorkspaceMemberResponse(
                100L,
                1L,
                "owner@example.com",
                "owner",
                WorkspaceRole.OWNER,
                LocalDateTime.of(2026, 3, 3, 9, 0)
        );
        WorkspaceMemberResponse member = new WorkspaceMemberResponse(
                101L,
                2L,
                "member@example.com",
                "member",
                WorkspaceRole.MEMBER,
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );
        given(workspaceService.getWorkspaceMembers(1L, 10L)).willReturn(List.of(owner, member));

        mockMvc.perform(get(WorkspaceHttpContract.membersPath(10L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].memberId").value(100L))
                .andExpect(jsonPath("$.data[1].role").value("MEMBER"));
    }

    @Test
    void updateWorkspaceReturnsResponse() throws Exception {
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest("TaskFlow Core", "core workspace");
        WorkspaceSummaryResponse response = new WorkspaceSummaryResponse(
                10L,
                "TaskFlow Core",
                "core workspace",
                WorkspaceRole.OWNER
        );

        given(workspaceService.updateWorkspace(eq(1L), eq(10L), any(UpdateWorkspaceRequest.class))).willReturn(response);

        mockMvc.perform(patch(WorkspaceHttpContract.detailPath(10L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value(10L))
                .andExpect(jsonPath("$.data.name").value("TaskFlow Core"))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"));
    }

    @Test
    void deleteWorkspaceReturnsResponse() throws Exception {
        mockMvc.perform(delete(WorkspaceHttpContract.detailPath(10L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(workspaceService).should().deleteWorkspace(1L, 10L);
    }

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
}
