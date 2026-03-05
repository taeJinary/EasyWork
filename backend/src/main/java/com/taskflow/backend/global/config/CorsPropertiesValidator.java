package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CorsPropertiesValidator {

    private static final String PROD_PROFILE = "prod";
    private final Environment environment;
    private final List<String> allowedOrigins;

    public CorsPropertiesValidator(
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
        for (String origin : allowedOrigins) {
            validateOrigin(origin);
        }
    }

    private void validateOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            throw new IllegalStateException("In prod profile, app.cors.allowed-origins must not contain blank values");
        }

        String normalizedOrigin = origin.trim();
        if ("*".equals(normalizedOrigin)) {
            throw new IllegalStateException("In prod profile, app.cors.allowed-origins cannot contain '*'");
        }

        URI uri;
        try {
            uri = URI.create(normalizedOrigin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "In prod profile, app.cors.allowed-origins contains invalid URI: " + normalizedOrigin
            );
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException(
                    "In prod profile, app.cors.allowed-origins must use https scheme: " + normalizedOrigin
            );
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException(
                    "In prod profile, app.cors.allowed-origins must include host: " + normalizedOrigin
            );
        }

        if (isLocalAddress(host)) {
            throw new IllegalStateException(
                    "In prod profile, app.cors.allowed-origins cannot use localhost address: " + normalizedOrigin
            );
        }
    }

    private boolean isLocalAddress(String host) {
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalizedHost)
                || "127.0.0.1".equals(normalizedHost)
                || "::1".equals(normalizedHost)
                || "[::1]".equals(normalizedHost);
    }

    private boolean isProdProfileActive() {
        return environment.acceptsProfiles(Profiles.of(PROD_PROFILE));
    }
}
