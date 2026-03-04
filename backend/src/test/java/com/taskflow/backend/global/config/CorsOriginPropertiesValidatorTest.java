package com.taskflow.backend.global.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsOriginPropertiesValidatorTest {

    @Test
    void nonProdProfileAllowsLocalHttpOrigins() {
        CorsOriginPropertiesValidator validator = new CorsOriginPropertiesValidator(
                environment("test"),
                List.of("http://localhost:5173", "http://127.0.0.1:5173")
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void prodProfileRejectsLocalhostOrigin() {
        CorsOriginPropertiesValidator validator = new CorsOriginPropertiesValidator(
                environment("prod"),
                List.of("http://localhost:5173")
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins");
    }

    @Test
    void prodProfileRejectsNonHttpsOrigin() {
        CorsOriginPropertiesValidator validator = new CorsOriginPropertiesValidator(
                environment("prod"),
                List.of("http://taskflow.example.com")
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins");
    }

    @Test
    void prodProfileAcceptsHttpsOrigins() {
        CorsOriginPropertiesValidator validator = new CorsOriginPropertiesValidator(
                environment("prod"),
                List.of("https://taskflow.example.com", "https://admin.taskflow.example.com")
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    private MockEnvironment environment(String... activeProfiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        return environment;
    }
}
