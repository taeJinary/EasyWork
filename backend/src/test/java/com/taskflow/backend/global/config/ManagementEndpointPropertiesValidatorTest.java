package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagementEndpointPropertiesValidatorTest {

    @Test
    void nonProdProfileAllowsAnyExposureSettings() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("test"),
                "*",
                "health",
                "always"
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void prodProfileRequiresHealthEndpointExposure() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("prod"),
                "info",
                "",
                "never"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("management.endpoints.web.exposure.include");
    }

    @Test
    void prodProfileRejectsWildcardExposure() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("prod"),
                "*",
                "",
                "never"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("management.endpoints.web.exposure.include");
    }

    @Test
    void prodProfileRequiresNeverHealthDetails() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("prod"),
                "health,info",
                "",
                "always"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("management.endpoint.health.show-details");
    }

    @Test
    void prodProfileWithExpectedManagementSettingsPassesValidation() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("prod"),
                "health,info",
                "",
                "never"
        );

        assertThatCode(validator::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void prodProfileRejectsHealthEndpointExcluded() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("prod"),
                "health,info",
                "health",
                "never"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("management.endpoints.web.exposure.exclude");
    }

    @Test
    void prodProfileRejectsWildcardExcluded() {
        ManagementEndpointPropertiesValidator validator = new ManagementEndpointPropertiesValidator(
                environment("prod"),
                "health,info",
                "*",
                "never"
        );

        assertThatThrownBy(validator::validateAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("management.endpoints.web.exposure.exclude");
    }

    private MockEnvironment environment(String... activeProfiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        return environment;
    }
}
