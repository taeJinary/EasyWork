package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookieSecurityPropertiesValidatorTest {

    @Test
    void nonProdProfileAllowsInsecureCookieSettings() {
        CookieSecurityPropertiesValidator validator = new CookieSecurityPropertiesValidator(
                environment("test"),
                false,
                "None",
                "/custom",
                "custom_refresh_token"
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void prodProfileRequiresSecureCookie() {
        CookieSecurityPropertiesValidator validator = new CookieSecurityPropertiesValidator(
                environment("prod"),
                false,
                "Lax",
                "/api/v1/auth",
                "refresh_token"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cookie.secure");
    }

    @Test
    void prodProfileRequiresLaxSameSite() {
        CookieSecurityPropertiesValidator validator = new CookieSecurityPropertiesValidator(
                environment("prod"),
                true,
                "None",
                "/api/v1/auth",
                "refresh_token"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cookie.same-site");
    }

    @Test
    void prodProfileRequiresApiV1AuthPath() {
        CookieSecurityPropertiesValidator validator = new CookieSecurityPropertiesValidator(
                environment("prod"),
                true,
                "Lax",
                "/auth",
                "refresh_token"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cookie.refresh-token-path");
    }

    @Test
    void prodProfileRequiresRefreshTokenName() {
        CookieSecurityPropertiesValidator validator = new CookieSecurityPropertiesValidator(
                environment("prod"),
                true,
                "Lax",
                "/api/v1/auth",
                "custom_refresh"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cookie.refresh-token-name");
    }

    @Test
    void prodProfileWithExpectedCookieSettingsPassesValidation() {
        CookieSecurityPropertiesValidator validator = new CookieSecurityPropertiesValidator(
                environment("prod"),
                true,
                "Lax",
                "/api/v1/auth",
                "refresh_token"
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    private MockEnvironment environment(String... activeProfiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        return environment;
    }
}
