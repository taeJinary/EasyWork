package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CookieSecurityPropertiesValidator {

    private static final String PROD_PROFILE = "prod";
    private static final String REQUIRED_SAME_SITE = "Lax";
    private static final String REQUIRED_REFRESH_TOKEN_PATH = "/api/v1/auth";
    private static final String REQUIRED_REFRESH_TOKEN_NAME = "refresh_token";

    private final Environment environment;
    private final boolean secure;
    private final String sameSite;
    private final String refreshTokenPath;
    private final String refreshTokenName;

    public CookieSecurityPropertiesValidator(
            Environment environment,
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.same-site:Lax}") String sameSite,
            @Value("${app.cookie.refresh-token-path:/api/v1/auth}") String refreshTokenPath,
            @Value("${app.cookie.refresh-token-name:refresh_token}") String refreshTokenName
    ) {
        this.environment = environment;
        this.secure = secure;
        this.sameSite = sameSite;
        this.refreshTokenPath = refreshTokenPath;
        this.refreshTokenName = refreshTokenName;
    }

    @PostConstruct
    public void validateAtStartup() {
        if (!isProdProfileActive()) {
            return;
        }
        if (!secure) {
            throw new IllegalStateException("In prod profile, app.cookie.secure must be true");
        }
        if (!REQUIRED_SAME_SITE.equalsIgnoreCase(sameSite)) {
            throw new IllegalStateException("In prod profile, app.cookie.same-site must be Lax");
        }
        if (!REQUIRED_REFRESH_TOKEN_PATH.equals(refreshTokenPath)) {
            throw new IllegalStateException("In prod profile, app.cookie.refresh-token-path must be /api/v1/auth");
        }
        if (!REQUIRED_REFRESH_TOKEN_NAME.equals(refreshTokenName)) {
            throw new IllegalStateException("In prod profile, app.cookie.refresh-token-name must be refresh_token");
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(PROD_PROFILE::equalsIgnoreCase);
    }
}
