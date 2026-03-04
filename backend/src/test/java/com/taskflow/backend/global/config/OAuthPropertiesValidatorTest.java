package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthPropertiesValidatorTest {

    @Test
    void nonProdProfileAllowsBlankOAuthSettings() {
        OAuthPropertiesValidator validator = new OAuthPropertiesValidator(
                environment("test"),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void prodProfileRequiresGoogleClientId() {
        OAuthPropertiesValidator validator = new OAuthPropertiesValidator(
                environment("prod"),
                "",
                "google-secret",
                "https://app.example.com/oauth/google",
                "kakao-id",
                "kakao-secret",
                "https://app.example.com/oauth/kakao",
                "naver-id",
                "naver-secret",
                "https://app.example.com/oauth/naver"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.oauth.google.client-id");
    }

    @Test
    void prodProfileRejectsNonHttpsGoogleRedirectUri() {
        OAuthPropertiesValidator validator = new OAuthPropertiesValidator(
                environment("prod"),
                "google-id",
                "google-secret",
                "http://app.example.com/oauth/google",
                "kakao-id",
                "kakao-secret",
                "https://app.example.com/oauth/kakao",
                "naver-id",
                "naver-secret",
                "https://app.example.com/oauth/naver"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.oauth.google.redirect-uri");
    }

    @Test
    void prodProfileRejectsLocalhostNaverRedirectUri() {
        OAuthPropertiesValidator validator = new OAuthPropertiesValidator(
                environment("prod"),
                "google-id",
                "google-secret",
                "https://app.example.com/oauth/google",
                "kakao-id",
                "kakao-secret",
                "https://app.example.com/oauth/kakao",
                "naver-id",
                "naver-secret",
                "https://localhost:5173/oauth/naver"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.oauth.naver.redirect-uri");
    }

    @Test
    void prodProfileRejectsWhitespacePaddedKakaoClientSecret() {
        OAuthPropertiesValidator validator = new OAuthPropertiesValidator(
                environment("prod"),
                "google-id",
                "google-secret",
                "https://app.example.com/oauth/google",
                "kakao-id",
                " kakao-secret ",
                "https://app.example.com/oauth/kakao",
                "naver-id",
                "naver-secret",
                "https://app.example.com/oauth/naver"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.oauth.kakao.client-secret");
    }

    @Test
    void prodProfileWithExpectedOAuthSettingsPassesValidation() {
        OAuthPropertiesValidator validator = new OAuthPropertiesValidator(
                environment("prod"),
                "google-id",
                "google-secret",
                "https://app.example.com/oauth/google",
                "kakao-id",
                "kakao-secret",
                "https://app.example.com/oauth/kakao",
                "naver-id",
                "naver-secret",
                "https://app.example.com/oauth/naver"
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    private MockEnvironment environment(String... activeProfiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        return environment;
    }
}
