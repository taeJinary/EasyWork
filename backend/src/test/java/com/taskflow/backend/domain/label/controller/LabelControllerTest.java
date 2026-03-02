package com.taskflow.backend.domain.label.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.domain.label.dto.request.CreateLabelRequest;
import com.taskflow.backend.domain.label.dto.request.UpdateLabelRequest;
import com.taskflow.backend.domain.label.dto.response.LabelResponse;
import com.taskflow.backend.domain.label.service.LabelService;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import com.taskflow.backend.global.common.enums.UserStatus;
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

@WebMvcTest(LabelController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LabelService labelService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createLabelReturnsCreatedResponse() throws Exception {
        CreateLabelRequest request = new CreateLabelRequest("Backend", "#2563EB");
        LabelResponse response = new LabelResponse(1L, "Backend", "#2563EB");
        given(labelService.createLabel(eq(1L), eq(10L), any(CreateLabelRequest.class))).willReturn(response);

        mockMvc.perform(post("/projects/10/labels")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.labelId").value(1L))
                .andExpect(jsonPath("$.data.name").value("Backend"));

        then(labelService).should().createLabel(eq(1L), eq(10L), any(CreateLabelRequest.class));
    }

    @Test
    void getLabelsReturnsResponse() throws Exception {
        given(labelService.getLabels(1L, 10L)).willReturn(List.of(
                new LabelResponse(1L, "Backend", "#2563EB"),
                new LabelResponse(2L, "Auth", "#16A34A")
        ));

        mockMvc.perform(get("/projects/10/labels")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].labelId").value(1L))
                .andExpect(jsonPath("$.data[1].name").value("Auth"));

        then(labelService).should().getLabels(1L, 10L);
    }

    @Test
    void updateLabelReturnsResponse() throws Exception {
        UpdateLabelRequest request = new UpdateLabelRequest("Auth", "#16A34A");
        LabelResponse response = new LabelResponse(1L, "Auth", "#16A34A");
        given(labelService.updateLabel(eq(1L), eq(1L), any(UpdateLabelRequest.class))).willReturn(response);

        mockMvc.perform(patch("/labels/1")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Auth"));

        then(labelService).should().updateLabel(eq(1L), eq(1L), any(UpdateLabelRequest.class));
    }

    @Test
    void deleteLabelReturnsResponse() throws Exception {
        mockMvc.perform(delete("/labels/1")
                        .principal(principalAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(labelService).should().deleteLabel(1L, 1L);
    }

    @Test
    void createLabelReturnsBadRequestWhenColorInvalid() throws Exception {
        String invalidJson = """
                {
                  "name": "Backend",
                  "colorHex": "blue"
                }
                """;

        mockMvc.perform(post("/projects/10/labels")
                        .principal(principalAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
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
