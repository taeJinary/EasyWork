package com.taskflow.backend.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.comment.dto.request.CreateCommentRequest;
import com.taskflow.backend.domain.comment.dto.request.UpdateCommentRequest;
import com.taskflow.backend.domain.comment.dto.response.CommentListResponse;
import com.taskflow.backend.domain.comment.dto.response.CommentResponse;
import com.taskflow.backend.domain.comment.service.CommentService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

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
    void createCommentReturnsCreatedResponse() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("로그인 실패");
        CommentResponse response = new CommentResponse(
                150L,
                new CommentResponse.AuthorResponse(1L, "owner"),
                "로그인 실패",
                LocalDateTime.of(2026, 3, 2, 10, 0),
                LocalDateTime.of(2026, 3, 2, 10, 0),
                true
        );
        given(commentService.createComment(eq(1L), eq(100L), any(CreateCommentRequest.class))).willReturn(response);

        mockMvc.perform(post(CommentHttpContract.taskCommentsPath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commentId").value(150L))
                .andExpect(jsonPath("$.data.author.userId").value(1L))
                .andExpect(jsonPath("$.data.content").value("로그인 실패"))
                .andExpect(jsonPath("$.data.editable").value(true));

        then(apiRateLimitService).should().checkCommentCreate(any(), eq(1L));
        then(commentService).should().createComment(eq(1L), eq(100L), any(CreateCommentRequest.class));
    }

    @Test
    void createCommentReturnsTooManyRequestsWhenRateLimited() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("rate limit");
        doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .when(apiRateLimitService)
                .checkCommentCreate(any(), eq(1L));

        mockMvc.perform(post(CommentHttpContract.taskCommentsPath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"));

        then(commentService).should(never()).createComment(eq(1L), eq(100L), any(CreateCommentRequest.class));
    }

    @Test
    void createCommentReturnsBadRequestWhenContentMissing() throws Exception {
        String invalidJson = """
                {
                  "content": " "
                }
                """;

        mockMvc.perform(post(CommentHttpContract.taskCommentsPath(100L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void getCommentsReturnsResponse() throws Exception {
        CommentResponse item = new CommentResponse(
                150L,
                new CommentResponse.AuthorResponse(1L, "owner"),
                "로그인 실패",
                LocalDateTime.of(2026, 3, 2, 10, 0),
                LocalDateTime.of(2026, 3, 2, 10, 0),
                true
        );
        CommentListResponse response = new CommentListResponse(
                List.of(item),
                150L,
                true
        );
        given(commentService.getComments(1L, 100L, 160L, 20)).willReturn(response);

        mockMvc.perform(get(CommentHttpContract.taskCommentsPath(100L))
                        .principal(principalAuth())
                        .param("cursor", "160")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].commentId").value(150L))
                .andExpect(jsonPath("$.data.content[0].editable").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value(150L))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        then(commentService).should().getComments(1L, 100L, 160L, 20);
    }

    @Test
    void updateCommentReturnsResponse() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest("수정된 댓글");
        CommentResponse response = new CommentResponse(
                150L,
                new CommentResponse.AuthorResponse(1L, "owner"),
                "수정된 댓글",
                LocalDateTime.of(2026, 3, 2, 10, 0),
                LocalDateTime.of(2026, 3, 2, 10, 5),
                true
        );
        given(commentService.updateComment(eq(1L), eq(150L), any(UpdateCommentRequest.class))).willReturn(response);

        mockMvc.perform(patch(CommentHttpContract.commentPath(150L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commentId").value(150L))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글"))
                .andExpect(jsonPath("$.data.editable").value(true));

        then(commentService).should().updateComment(eq(1L), eq(150L), any(UpdateCommentRequest.class));
    }

    @Test
    void updateCommentReturnsBadRequestWhenContentMissing() throws Exception {
        String invalidJson = """
                {
                  "content": " "
                }
                """;

        mockMvc.perform(patch(CommentHttpContract.commentPath(150L))
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void deleteCommentReturnsResponse() throws Exception {
        mockMvc.perform(delete(CommentHttpContract.commentPath(150L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(commentService).should().deleteComment(1L, 150L);
    }
}
