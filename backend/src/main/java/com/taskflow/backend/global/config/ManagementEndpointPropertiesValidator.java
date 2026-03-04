package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ManagementEndpointPropertiesValidator {

    private static final String PROD_PROFILE = "prod";
    private static final String HEALTH_ENDPOINT = "health";
    private static final String REQUIRED_HEALTH_SHOW_DETAILS = "never";

    private final Environment environment;
    private final String exposureInclude;
    private final String exposureExclude;
    private final String healthShowDetails;

    public ManagementEndpointPropertiesValidator(
            Environment environment,
            @Value("${management.endpoints.web.exposure.include:health,info}") String exposureInclude,
            @Value("${management.endpoints.web.exposure.exclude:}") String exposureExclude,
            @Value("${management.endpoint.health.show-details:never}") String healthShowDetails
    ) {
        this.environment = environment;
        this.exposureInclude = exposureInclude;
        this.exposureExclude = exposureExclude;
        this.healthShowDetails = healthShowDetails;
    }

    @PostConstruct
    public void validateAtStartup() {
        if (!isProdProfileActive()) {
            return;
        }

        Set<String> exposedEndpoints = Arrays.stream(exposureInclude.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> excludedEndpoints = Arrays.stream(exposureExclude.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (exposedEndpoints.contains("*")) {
            throw new IllegalStateException(
                    "In prod profile, management.endpoints.web.exposure.include cannot contain '*'");
        }
        if (!exposedEndpoints.contains(HEALTH_ENDPOINT)) {
            throw new IllegalStateException(
                    "In prod profile, management.endpoints.web.exposure.include must contain health");
        }
        if (excludedEndpoints.contains("*") || excludedEndpoints.contains(HEALTH_ENDPOINT)) {
            throw new IllegalStateException(
                    "In prod profile, management.endpoints.web.exposure.exclude cannot disable health");
        }
        if (!REQUIRED_HEALTH_SHOW_DETAILS.equalsIgnoreCase(healthShowDetails.trim())) {
            throw new IllegalStateException(
                    "In prod profile, management.endpoint.health.show-details must be never");
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(PROD_PROFILE::equalsIgnoreCase);
    }
}
