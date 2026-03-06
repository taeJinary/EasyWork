package com.taskflow.backend.domain.attachment.controller;

import com.taskflow.backend.domain.attachment.dto.response.TaskAttachmentResponse;
import com.taskflow.backend.domain.attachment.service.TaskAttachmentService;
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
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskAttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskAttachmentService taskAttachmentService;

    @MockBean
    private ApiRateLimitService apiRateLimitService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void uploadAttachmentReturnsCreatedResponse() throws Exception {
        TaskAttachmentResponse response = attachmentResponse(2000L, 100L, "report.pdf");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "hello".getBytes()
        );

        given(taskAttachmentService.uploadAttachment(eq(1L), eq(100L), any())).willReturn(response);

        mockMvc.perform(multipart(AttachmentHttpContract.taskAttachmentsPath(100L))
                        .file(file)
                        .principal(principalAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attachmentId").value(2000L))
                .andExpect(jsonPath("$.data.taskId").value(100L))
                .andExpect(jsonPath("$.data.originalFilename").value("report.pdf"));

        then(apiRateLimitService).should().checkAttachmentUpload(any(), eq(1L));
        then(taskAttachmentService).should().uploadAttachment(eq(1L), eq(100L), any());
    }

    @Test
    void uploadAttachmentReturnsTooManyRequestsWhenRateLimited() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "hello".getBytes()
        );
        doThrow(new BusinessException(ErrorCode.TOO_MANY_REQUESTS))
                .when(apiRateLimitService)
                .checkAttachmentUpload(any(), eq(1L));

        mockMvc.perform(multipart(AttachmentHttpContract.taskAttachmentsPath(100L))
                        .file(file)
                        .principal(principalAuth()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"));

        then(taskAttachmentService).should(never()).uploadAttachment(eq(1L), eq(100L), any());
    }

    @Test
    void getTaskAttachmentsReturnsOkResponse() throws Exception {
        given(taskAttachmentService.getTaskAttachments(1L, 100L)).willReturn(List.of(
                attachmentResponse(2000L, 100L, "report.pdf"),
                attachmentResponse(2001L, 100L, "spec.docx")
        ));

        mockMvc.perform(get(AttachmentHttpContract.taskAttachmentsPath(100L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].attachmentId").value(2000L))
                .andExpect(jsonPath("$.data[1].originalFilename").value("spec.docx"));

        then(taskAttachmentService).should().getTaskAttachments(1L, 100L);
    }

    @Test
    void deleteAttachmentReturnsOkResponse() throws Exception {
        mockMvc.perform(delete(AttachmentHttpContract.attachmentPath(2000L))
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(taskAttachmentService).should().deleteAttachment(1L, 2000L);
    }

    @Test
    void uploadAttachmentReturnsBadRequestWhenMissingFilePart() throws Exception {
        mockMvc.perform(multipart(AttachmentHttpContract.taskAttachmentsPath(100L))
                        .principal(principalAuth()))
                .andExpect(status().isBadRequest());
    }

    private TaskAttachmentResponse attachmentResponse(Long attachmentId, Long taskId, String originalFilename) {
        return new TaskAttachmentResponse(
                attachmentId,
                taskId,
                originalFilename,
                "application/pdf",
                5L,
                1L,
                "uploader",
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );
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
