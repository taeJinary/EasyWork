package com.taskflow.backend.global.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesValidatorTest {

    @Test
    void skipsValidationOutsideProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                devEnvironment(),
                List.of("http://localhost:5173", "http://127.0.0.1:5173")
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void allowsHttpsOriginsInProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                prodEnvironment(),
                List.of("https://app.easywork.com", "https://admin.easywork.com")
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyAllowedOriginsInProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                prodEnvironment(),
                List.of()
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins must not be empty");
    }

    @Test
    void rejectsWildcardAllowedOriginsInProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                prodEnvironment(),
                List.of("*")
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins cannot contain '*'");
    }

    @Test
    void rejectsNonHttpsOriginsInProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                prodEnvironment(),
                List.of("http://app.easywork.com")
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins must use https scheme");
    }

    @Test
    void rejectsLocalhostOriginInProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                prodEnvironment(),
                List.of("https://localhost:5173")
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins cannot use localhost address");
    }

    @Test
    void rejectsInvalidUriInProdProfile() {
        CorsPropertiesValidator validator = new CorsPropertiesValidator(
                prodEnvironment(),
                List.of("https://exa mple.com")
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins contains invalid URI");
    }

    private MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private MockEnvironment devEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        return environment;
    }
}
