package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class CorsOriginPropertiesValidator {

    private static final String PROD_PROFILE = "prod";

    private final Environment environment;
    private final List<String> allowedOrigins;

    public CorsOriginPropertiesValidator(
            Environment environment,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") List<String> allowedOrigins
    ) {
        this.environment = environment;
        this.allowedOrigins = allowedOrigins;
    }

    @PostConstruct
    public void validateAtStartup() {
        if (!isProdProfileActive()) {
            return;
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("In prod profile, app.cors.allowed-origins must not be empty");
        }

        for (String allowedOrigin : allowedOrigins) {
            String normalized = allowedOrigin == null ? "" : allowedOrigin.trim();
            String lowered = normalized.toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                throw new IllegalStateException("In prod profile, app.cors.allowed-origins must not contain blank value");
            }
            if ("*".equals(lowered)) {
                throw new IllegalStateException("In prod profile, app.cors.allowed-origins cannot contain '*'");
            }
            if (lowered.contains("localhost") || lowered.contains("127.0.0.1")) {
                throw new IllegalStateException("In prod profile, app.cors.allowed-origins must not use localhost");
            }
            if (!lowered.startsWith("https://")) {
                throw new IllegalStateException("In prod profile, app.cors.allowed-origins must use https");
            }
        }
    }

    private boolean isProdProfileActive() {
        return environment.acceptsProfiles(Profiles.of(PROD_PROFILE));
    }
}
