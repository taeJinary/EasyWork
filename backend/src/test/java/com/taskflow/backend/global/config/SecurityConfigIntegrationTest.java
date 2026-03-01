package com.taskflow.backend.global.config;

import com.taskflow.backend.support.IntegrationTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersMeEndpointReturns401ForAnonymousRequest() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectsEndpointReturns401ForAnonymousRequest() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invitationsEndpointReturns401ForAnonymousRequest() throws Exception {
        mockMvc.perform(get("/invitations/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfoEndpointPermitsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }
}
