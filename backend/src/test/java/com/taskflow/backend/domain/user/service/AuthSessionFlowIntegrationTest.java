package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.controller.AuthHttpContract;
import com.jayway.jsonpath.JsonPath;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.repository.EmailVerificationRetryJobRepository;
import com.taskflow.backend.domain.user.repository.EmailVerificationTokenRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.user.service.EmailVerificationTokenGenerator;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private EmailVerificationRetryJobRepository emailVerificationRetryJobRepository;

    @MockBean
    private EmailVerificationTokenGenerator emailVerificationTokenGenerator;

    @MockBean
    private EmailVerificationMailService emailVerificationMailService;

    @Test
    void loginReissueLogoutBlocksReissueAndBlacklistedAccessTokenReuse() throws Exception {
        String email = "auth-flow-" + System.nanoTime() + "@example.com";
        String password = "Pass123!";
        org.mockito.BDDMockito.given(emailVerificationTokenGenerator.generate()).willReturn("fixed-email-token");
        org.mockito.BDDMockito.given(emailVerificationMailService.isReady()).willReturn(true);

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

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_NOT_VERIFIED"));

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH
                        + AuthHttpContract.EMAIL_VERIFICATION_BASE_PATH
                        + AuthHttpContract.EMAIL_VERIFICATION_VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "fixed-email-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

        MvcResult loginResult = mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME + "=")))
                .andExpect(header().string("Set-Cookie", containsString("Path=" + AuthHttpContract.REFRESH_TOKEN_COOKIE_PATH)))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=" + AuthHttpContract.REFRESH_TOKEN_COOKIE_SAME_SITE)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        String firstAccessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");
        String firstRefreshToken = extractCookieValue(
                loginResult.getResponse().getHeader("Set-Cookie"),
                AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME
        );
        assertThat(firstRefreshToken).isNotBlank();

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        MvcResult reissueResult = mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.TOKEN_REISSUE_PATH)
                        .cookie(new Cookie(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME, firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME + "=")))
                .andExpect(header().string("Set-Cookie", containsString("Path=" + AuthHttpContract.REFRESH_TOKEN_COOKIE_PATH)))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=" + AuthHttpContract.REFRESH_TOKEN_COOKIE_SAME_SITE)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        String secondAccessToken = JsonPath.read(reissueResult.getResponse().getContentAsString(), "$.data.accessToken");
        String secondRefreshToken = extractCookieValue(
                reissueResult.getResponse().getHeader("Set-Cookie"),
                AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME
        );
        assertThat(secondRefreshToken).isNotBlank();
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.TOKEN_REISSUE_PATH)
                        .cookie(new Cookie(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME, firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + secondAccessToken)
                        .cookie(new Cookie(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME, secondRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Path=" + AuthHttpContract.REFRESH_TOKEN_COOKIE_PATH)))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=" + AuthHttpContract.REFRESH_TOKEN_COOKIE_SAME_SITE)))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH + AuthHttpContract.TOKEN_REISSUE_PATH)
                        .cookie(new Cookie(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME, secondRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_INVALID"));

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendEmailVerificationReturnsTooManyRequestsWhenCooldownIsActive() throws Exception {
        String email = "auth-resend-" + System.nanoTime() + "@example.com";
        org.mockito.BDDMockito.given(emailVerificationTokenGenerator.generate())
                .willReturn("initial-email-token", "resend-email-token");
        org.mockito.BDDMockito.given(emailVerificationMailService.isReady()).willReturn(true);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Pass123!",
                                  "nickname": "authresend"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH
                        + AuthHttpContract.EMAIL_VERIFICATION_BASE_PATH
                        + AuthHttpContract.EMAIL_VERIFICATION_RESEND_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 메일을 다시 보냈습니다."));

        mockMvc.perform(post(AuthHttpContract.AUTH_BASE_PATH
                        + AuthHttpContract.EMAIL_VERIFICATION_BASE_PATH
                        + AuthHttpContract.EMAIL_VERIFICATION_RESEND_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_VERIFICATION_RESEND_TOO_FREQUENT"));
    }

    @Test
    void resendEmailVerificationPreservesExistingTokenWhenMailSendFails() {
        String email = "auth-resend-fail-" + System.nanoTime() + "@example.com";
        org.mockito.BDDMockito.given(emailVerificationTokenGenerator.generate())
                .willReturn("initial-email-token", "resend-email-token");
        org.mockito.BDDMockito.given(emailVerificationMailService.isReady()).willReturn(true);

        authService.signup(new SignupRequest(email, "Pass123!", "authresendfail"));

        org.mockito.BDDMockito.willThrow(new RuntimeException("smtp down"))
                .given(emailVerificationMailService)
                .sendVerificationEmail(email, "resend-email-token");

        authService.resendEmailVerification(email);

        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        assertThat(emailVerificationRetryJobRepository.existsByUserIdAndCompletedAtIsNull(userId)).isTrue();
        assertThat(emailVerificationTokenRepository.findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(userId))
                .hasSize(1);

        authService.verifyEmail("initial-email-token");

        assertThat(userRepository.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();
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
