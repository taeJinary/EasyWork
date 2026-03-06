package com.taskflow.backend.global.config;

import com.taskflow.backend.domain.user.controller.AuthHttpContract;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class CookieSecurityPropertiesValidator {

    private static final String PROD_PROFILE = "prod";
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
        validateCookieContract();

        if (isProdProfileActive() && !secure) {
            throw new IllegalStateException("In prod profile, app.cookie.secure must be true");
        }
    }

    private void validateCookieContract() {
        if (!AuthHttpContract.REFRESH_TOKEN_COOKIE_SAME_SITE.equalsIgnoreCase(sameSite)) {
            throw new IllegalStateException("app.cookie.same-site must be Lax");
        }
        if (!AuthHttpContract.REFRESH_TOKEN_COOKIE_PATH.equals(refreshTokenPath)) {
            throw new IllegalStateException("app.cookie.refresh-token-path must be /api/v1/auth");
        }
        if (!AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME.equals(refreshTokenName)) {
            throw new IllegalStateException("app.cookie.refresh-token-name must be refresh_token");
        }
    }

    private boolean isProdProfileActive() {
        return environment.acceptsProfiles(Profiles.of(PROD_PROFILE));
    }
}
