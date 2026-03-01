package com.taskflow.backend.domain.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectDetailResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListItemResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.service.ProjectService;
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

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

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
    void createProjectReturnsCreatedResponse() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("TaskFlow", "협업용 태스크 관리 프로젝트");
        ProjectSummaryResponse response = new ProjectSummaryResponse(
                10L,
                "TaskFlow",
                "협업용 태스크 관리 프로젝트",
                ProjectRole.OWNER
        );
        given(projectService.createProject(eq(1L), any(CreateProjectRequest.class))).willReturn(response);

        mockMvc.perform(post("/projects")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.name").value("TaskFlow"))
                .andExpect(jsonPath("$.message").value("프로젝트가 생성되었습니다."));
    }

    @Test
    void getMyProjectsReturnsPagedResponse() throws Exception {
        ProjectListItemResponse item = new ProjectListItemResponse(
                10L,
                "TaskFlow",
                "협업용 태스크 관리 프로젝트",
                ProjectRole.OWNER,
                3L,
                0L,
                0L,
                0,
                LocalDateTime.of(2026, 3, 1, 10, 30)
        );
        ProjectListResponse response = new ProjectListResponse(
                List.of(item),
                0,
                20,
                1L,
                1,
                true,
                true
        );
        given(projectService.getMyProjects(1L, 0, 20)).willReturn(response);

        mockMvc.perform(get("/projects?page=0&size=20")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].projectId").value(10L))
                .andExpect(jsonPath("$.data.content[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getProjectDetailReturnsPayload() throws Exception {
        ProjectDetailResponse.MemberResponse owner = new ProjectDetailResponse.MemberResponse(
                100L,
                1L,
                "owner@example.com",
                "오너",
                ProjectRole.OWNER,
                LocalDateTime.of(2026, 3, 1, 9, 0)
        );
        ProjectDetailResponse response = new ProjectDetailResponse(
                10L,
                "TaskFlow",
                "협업용 태스크 관리 프로젝트",
                ProjectRole.OWNER,
                1L,
                0L,
                new ProjectDetailResponse.TaskSummaryResponse(0L, 0L, 0L),
                List.of(owner)
        );

        given(projectService.getProjectDetail(1L, 10L)).willReturn(response);

        mockMvc.perform(get("/projects/10").principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.members[0].memberId").value(100L));
    }
}

