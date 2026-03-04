package com.taskflow.backend.domain.user.service;

import com.jayway.jsonpath.JsonPath;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionFlowIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReissueLogoutBlocksReissueAndBlacklistedAccessTokenReuse() throws Exception {
        String email = "auth-flow-" + System.nanoTime() + "@example.com";
        String password = "Pass123!";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nickname": "authflow"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        String firstAccessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");
        String firstRefreshToken = extractCookieValue(loginResult.getResponse().getHeader("Set-Cookie"), "refresh_token");
        assertThat(firstRefreshToken).isNotBlank();

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        MvcResult reissueResult = mockMvc.perform(post("/auth/token/reissue")
                        .cookie(new Cookie("refresh_token", firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        String secondAccessToken = JsonPath.read(reissueResult.getResponse().getContentAsString(), "$.data.accessToken");
        String secondRefreshToken = extractCookieValue(reissueResult.getResponse().getHeader("Set-Cookie"), "refresh_token");
        assertThat(secondRefreshToken).isNotBlank();
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/auth/token/reissue")
                        .cookie(new Cookie("refresh_token", firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + secondAccessToken)
                        .cookie(new Cookie("refresh_token", secondRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        mockMvc.perform(post("/auth/token/reissue")
                        .cookie(new Cookie("refresh_token", secondRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isUnauthorized());
    }

    private String extractCookieValue(String setCookieHeader, String cookieName) {
        if (setCookieHeader == null || setCookieHeader.isBlank()) {
            return "";
        }

        String prefix = cookieName + "=";
        int start = setCookieHeader.indexOf(prefix);
        if (start < 0) {
            return "";
        }

        int valueStart = start + prefix.length();
        int valueEnd = setCookieHeader.indexOf(';', valueStart);
        if (valueEnd < 0) {
            valueEnd = setCookieHeader.length();
        }
        return setCookieHeader.substring(valueStart, valueEnd);
    }
}
