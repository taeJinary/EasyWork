package com.taskflow.backend.global.config;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationMailPropertiesValidator {

    private static final String PROD_PROFILE = "prod";

    private final Environment environment;
    private final boolean emailVerificationEnabled;
    private final String fromAddress;
    private final String subjectPrefix;
    private final String verifyBaseUrl;
    private final boolean mailSenderAvailable;

    public EmailVerificationMailPropertiesValidator(
            Environment environment,
            @Value("${app.email-verification.enabled:false}") boolean emailVerificationEnabled,
            @Value("${app.email-verification.from:noreply@taskflow.local}") String fromAddress,
            @Value("${app.email-verification.subject-prefix:[TaskFlow]}") String subjectPrefix,
            @Value("${app.email-verification.verify-base-url:http://localhost:5173/verify-email}") String verifyBaseUrl,
            ObjectProvider<JavaMailSender> javaMailSenderProvider
    ) {
        this.environment = environment;
        this.emailVerificationEnabled = emailVerificationEnabled;
        this.fromAddress = fromAddress;
        this.subjectPrefix = subjectPrefix;
        this.verifyBaseUrl = verifyBaseUrl;
        this.mailSenderAvailable = javaMailSenderProvider.getIfAvailable() != null;
    }

    @PostConstruct
    public void validateAtStartup() {
        if (!isProdProfileActive() || !emailVerificationEnabled) {
            return;
        }

        requireNonBlankAndNoPadding("app.email-verification.from", fromAddress);
        requireNonBlankAndNoPadding("app.email-verification.subject-prefix", subjectPrefix);
        requireNonBlankAndNoPadding("app.email-verification.verify-base-url", verifyBaseUrl);
        requireHttpsAndNonLocalhostUrl("app.email-verification.verify-base-url", verifyBaseUrl);

        if (!mailSenderAvailable) {
            throw new IllegalStateException(
                    "In prod profile, spring.mail.* must configure a JavaMailSender when app.email-verification.enabled=true");
        }
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

    private void requireHttpsAndNonLocalhostUrl(String property, String value) {
        URI uri = parseUri(property, value);
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            throw new IllegalStateException("In prod profile, " + property + " must use https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("In prod profile, " + property + " must define a valid host");
        }

        String normalizedHost = normalizeHost(host);
        if (isLoopbackHost(normalizedHost)) {
            throw new IllegalStateException("In prod profile, " + property + " must not use localhost");
        }
    }

    private URI parseUri(String property, String value) {
        try {
            return new URI(value.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "In prod profile, " + property + " must be a valid absolute URI",
                    exception
            );
        }
    }

    private String normalizeHost(String host) {
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isLoopbackHost(String host) {
        if ("localhost".equals(host)) {
            return true;
        }

        if (!looksLikeIpLiteral(host)) {
            return false;
        }

        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean looksLikeIpLiteral(String host) {
        return host.matches("[0-9.]+") || host.contains(":");
    }

    private boolean isProdProfileActive() {
        return environment.acceptsProfiles(Profiles.of(PROD_PROFILE));
    }
}
