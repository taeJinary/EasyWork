package com.taskflow.backend.domain.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.request.MoveTaskRequest;
import com.taskflow.backend.domain.task.dto.request.UpdateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListItemResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
import com.taskflow.backend.domain.task.dto.response.TaskMoveResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.service.TaskService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDate;
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
                List.of(1L, 2L)
        );

        TaskSummaryResponse response = new TaskSummaryResponse(
                100L,
                10L,
                "로그인 API 구현",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                0,
                0L,
                new TaskSummaryResponse.AssigneeResponse(2L, "팀원")
        );
        given(taskService.createTask(eq(1L), eq(10L), any(CreateTaskRequest.class))).willReturn(response);

        mockMvc.perform(post(TaskHttpContract.projectTasksPath(10L))
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
                .andExpect(jsonPath("$.data.assignee.userId").value(2L));

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

        mockMvc.perform(get(TaskHttpContract.projectTaskBoardPath(10L))
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

        mockMvc.perform(get(TaskHttpContract.projectTasksPath(10L))
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

    @Test
    void getTaskDetailReturnsResponse() throws Exception {
        TaskDetailResponse response = new TaskDetailResponse(
                100L,
                10L,
                "로그인 API 구현",
                "Access/Refresh 구조 구현",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDate.of(2026, 3, 10),
                0,
                0L,
                new TaskDetailResponse.UserSummaryResponse(1L, "오너"),
                new TaskDetailResponse.UserSummaryResponse(2L, "팀원"),
                List.of(new TaskDetailResponse.LabelResponse(1L, "백엔드", "#2563EB")),
                0L,
                List.of(),
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 1, 9, 0)
        );

        given(taskService.getTaskDetail(1L, 100L)).willReturn(response);

        mockMvc.perform(get(TaskHttpContract.taskDetailPath(100L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(100L))
                .andExpect(jsonPath("$.data.projectId").value(10L))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.creator.userId").value(1L))
                .andExpect(jsonPath("$.data.assignee.userId").value(2L))
                .andExpect(jsonPath("$.data.labels[0].labelId").value(1L))
                .andExpect(jsonPath("$.data.commentCount").value(0));

        then(taskService).should().getTaskDetail(1L, 100L);
    }

    @Test
    void updateTaskReturnsResponse() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "로그인 API 및 재발급 구현",
                "설명을 수정했습니다.",
                2L,
                TaskPriority.URGENT,
                LocalDate.of(2026, 3, 11),
                List.of(1L, 3L),
                0L
        );

        TaskDetailResponse response = new TaskDetailResponse(
                100L,
                10L,
                "로그인 API 및 재발급 구현",
                "설명을 수정했습니다.",
                TaskStatus.TODO,
                TaskPriority.URGENT,
                LocalDate.of(2026, 3, 11),
                0,
                1L,
                new TaskDetailResponse.UserSummaryResponse(1L, "오너"),
                new TaskDetailResponse.UserSummaryResponse(2L, "팀원"),
                List.of(
                        new TaskDetailResponse.LabelResponse(1L, "백엔드", "#2563EB"),
                        new TaskDetailResponse.LabelResponse(3L, "인증", "#16A34A")
                ),
                0L,
                List.of(),
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 1, 9, 10)
        );
        given(taskService.updateTask(eq(1L), eq(100L), any(UpdateTaskRequest.class))).willReturn(response);

        mockMvc.perform(patch(TaskHttpContract.taskDetailPath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(100L))
                .andExpect(jsonPath("$.data.priority").value("URGENT"))
                .andExpect(jsonPath("$.data.labels[1].labelId").value(3L))
                .andExpect(jsonPath("$.message").value("태스크가 수정되었습니다."));

        then(taskService).should().updateTask(eq(1L), eq(100L), any(UpdateTaskRequest.class));
    }

    @Test
    void updateTaskReturnsBadRequestWhenVersionMissing() throws Exception {
        String invalidJson = """
                {
                  "title": "로그인 API 및 재발급 구현",
                  "description": "설명을 수정했습니다.",
                  "assigneeUserId": 2,
                  "priority": "URGENT",
                  "dueDate": "2026-03-11",
                  "labelIds": [1, 3]
                }
                """;

        mockMvc.perform(patch(TaskHttpContract.taskDetailPath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void moveTaskReturnsResponse() throws Exception {
        MoveTaskRequest request = new MoveTaskRequest(TaskStatus.IN_PROGRESS, 0, 0L);
        TaskMoveResponse response = new TaskMoveResponse(
                100L,
                TaskStatus.IN_PROGRESS,
                0,
                1L,
                null
        );
        given(taskService.moveTask(eq(1L), eq(100L), any(MoveTaskRequest.class))).willReturn(response);

        mockMvc.perform(patch(TaskHttpContract.taskMovePath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(100L))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.position").value(0))
                .andExpect(jsonPath("$.data.version").value(1));

        then(taskService).should().moveTask(eq(1L), eq(100L), any(MoveTaskRequest.class));
    }

    @Test
    void moveTaskReturnsBadRequestWhenVersionMissing() throws Exception {
        String invalidJson = """
                {
                  "toStatus": "IN_PROGRESS",
                  "targetPosition": 0
                }
                """;

        mockMvc.perform(patch(TaskHttpContract.taskMovePath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void deleteTaskReturnsOkResponse() throws Exception {
        mockMvc.perform(delete(TaskHttpContract.taskDetailPath(100L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(taskService).should().deleteTask(1L, 100L);
    }
}
