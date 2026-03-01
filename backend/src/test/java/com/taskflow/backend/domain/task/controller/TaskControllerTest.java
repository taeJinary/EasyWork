package com.taskflow.backend.domain.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListItemResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDate;
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

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

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
    void createTaskReturnsCreatedResponse() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "로그인 API 구현",
                "Access/Refresh 구조 구현",
                2L,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 10),
                java.util.List.of(1L, 2L)
        );

        TaskSummaryResponse.AssigneeResponse assignee = new TaskSummaryResponse.AssigneeResponse(2L, "팀원");
        TaskSummaryResponse response = new TaskSummaryResponse(
                100L,
                10L,
                "로그인 API 구현",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                0,
                0L,
                assignee
        );
        given(taskService.createTask(eq(1L), eq(10L), any(CreateTaskRequest.class))).willReturn(response);

        mockMvc.perform(post("/projects/10/tasks")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(100L))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.position").value(0))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.assignee.userId").value(2L))
                .andExpect(jsonPath("$.message").value("태스크가 생성되었습니다."));

        then(taskService).should().createTask(eq(1L), eq(10L), any(CreateTaskRequest.class));
    }

    @Test
    void getTaskBoardReturnsResponse() throws Exception {
        TaskBoardResponse.TaskCardResponse todoCard = new TaskBoardResponse.TaskCardResponse(
                100L,
                "로그인 API 구현",
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 10),
                0,
                0L,
                new TaskBoardResponse.AssigneeResponse(2L, "팀원"),
                List.of(new TaskBoardResponse.LabelResponse(1L, "백엔드", "#2563EB")),
                0L
        );

        TaskBoardResponse response = new TaskBoardResponse(
                10L,
                new TaskBoardResponse.FilterResponse(2L, TaskPriority.HIGH, 1L, "API"),
                List.of(
                        new TaskBoardResponse.ColumnResponse(TaskStatus.TODO, List.of(todoCard)),
                        new TaskBoardResponse.ColumnResponse(TaskStatus.IN_PROGRESS, List.of()),
                        new TaskBoardResponse.ColumnResponse(TaskStatus.DONE, List.of())
                )
        );

        given(taskService.getTaskBoard(1L, 10L, 2L, TaskPriority.HIGH, 1L, "API")).willReturn(response);

        mockMvc.perform(get("/projects/10/tasks/board")
                        .principal(principalAuth())
                        .param("assigneeUserId", "2")
                        .param("priority", "HIGH")
                        .param("labelId", "1")
                        .param("keyword", "API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.columns[0].status").value("TODO"))
                .andExpect(jsonPath("$.data.columns[0].tasks[0].taskId").value(100L))
                .andExpect(jsonPath("$.data.columns[0].tasks[0].labels[0].labelId").value(1L))
                .andExpect(jsonPath("$.data.columns[0].tasks[0].commentCount").value(0));

        then(taskService).should().getTaskBoard(1L, 10L, 2L, TaskPriority.HIGH, 1L, "API");
    }

    @Test
    void getTasksReturnsPagedResponse() throws Exception {
        TaskListItemResponse item = new TaskListItemResponse(
                100L,
                "로그인 API 구현",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 10),
                0,
                0L,
                new TaskListItemResponse.AssigneeResponse(2L, "팀원")
        );
        TaskListResponse response = new TaskListResponse(
                List.of(item),
                0,
                20,
                1L,
                1,
                true,
                true
        );

        given(taskService.getTasks(1L, 10L, 0, 20, TaskStatus.TODO, "updatedAt", "DESC", "로그인"))
                .willReturn(response);

        mockMvc.perform(get("/projects/10/tasks")
                        .principal(principalAuth())
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "TODO")
                        .param("sortBy", "updatedAt")
                        .param("direction", "DESC")
                        .param("keyword", "로그인"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].taskId").value(100L))
                .andExpect(jsonPath("$.data.content[0].status").value("TODO"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        then(taskService).should().getTasks(1L, 10L, 0, 20, TaskStatus.TODO, "updatedAt", "DESC", "로그인");
    }
}
