package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class OAuthPropertiesValidator {

    private static final String PROD_PROFILE = "prod";

    private final Environment environment;
    private final String googleClientId;
    private final String googleClientSecret;
    private final String googleRedirectUri;
    private final String naverClientId;
    private final String naverClientSecret;
    private final String naverRedirectUri;

    public OAuthPropertiesValidator(
            Environment environment,
            @Value("${app.oauth.google.client-id:}") String googleClientId,
            @Value("${app.oauth.google.client-secret:}") String googleClientSecret,
            @Value("${app.oauth.google.redirect-uri:}") String googleRedirectUri,
            @Value("${app.oauth.naver.client-id:}") String naverClientId,
            @Value("${app.oauth.naver.client-secret:}") String naverClientSecret,
            @Value("${app.oauth.naver.redirect-uri:}") String naverRedirectUri
    ) {
        this.environment = environment;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.googleRedirectUri = googleRedirectUri;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.naverRedirectUri = naverRedirectUri;
    }

    @PostConstruct
    public void validateAtStartup() {
        if (!isProdProfileActive()) {
            return;
        }

        validateProvider("google", googleClientId, googleClientSecret, googleRedirectUri);
        validateProvider("naver", naverClientId, naverClientSecret, naverRedirectUri);
    }

    private void validateProvider(
            String provider,
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
        requireNonBlankAndNoPadding(property(provider, "client-id"), clientId);
        requireNonBlankAndNoPadding(property(provider, "client-secret"), clientSecret);
        requireNonBlankAndNoPadding(property(provider, "redirect-uri"), redirectUri);
        requireHttpsAndNonLocalhostRedirectUri(property(provider, "redirect-uri"), redirectUri);
    }

    private void requireNonBlankAndNoPadding(String property, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("In prod profile, " + property + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalStateException(
                    "In prod profile, " + property + " must not contain leading or trailing whitespace");
        }
    }

    private void requireHttpsAndNonLocalhostRedirectUri(String property, String redirectUri) {
        String normalized = redirectUri.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("https://")) {
            throw new IllegalStateException("In prod profile, " + property + " must use https");
        }
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            throw new IllegalStateException("In prod profile, " + property + " must not use localhost");
        }
    }

    private String property(String provider, String key) {
        return "app.oauth." + provider + "." + key;
    }

    private boolean isProdProfileActive() {
        return environment.acceptsProfiles(Profiles.of(PROD_PROFILE));
    }
}
