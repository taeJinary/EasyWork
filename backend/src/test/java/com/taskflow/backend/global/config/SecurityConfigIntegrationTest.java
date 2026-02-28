package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigIntegrationTest.TestController.class)
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 인증_엔드포인트는_비인증_요청도_허용한다() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void 보호_엔드포인트는_비인증_요청시_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/private/ping"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class TestController {

        @GetMapping("/api/auth/ping")
        public ResponseEntity<String> authPing() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/api/private/ping")
        public ResponseEntity<String> privatePing() {
            return ResponseEntity.ok("ok");
        }
    }
}
