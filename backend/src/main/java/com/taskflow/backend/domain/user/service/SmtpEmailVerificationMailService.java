package com.taskflow.backend.domain.user.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailVerificationMailService implements EmailVerificationMailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${app.email-verification.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email-verification.from:noreply@taskflow.local}")
    private String fromAddress;

    @Value("${app.email-verification.subject-prefix:[TaskFlow]}")
    private String subjectPrefix;

    @Value("${app.email-verification.verify-base-url:http://localhost:5173/verify-email}")
    private String verifyBaseUrl;

    @Override
    public boolean isReady() {
        return emailEnabled && javaMailSenderProvider.getIfAvailable() != null;
    }

    @Override
    public void sendVerificationEmail(String recipientEmail, String rawToken) {
        if (!isReady()) {
            return;
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();

        if (!StringUtils.hasText(recipientEmail) || !StringUtils.hasText(rawToken)) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject(subjectPrefix + " Email verification");
        message.setText("""
                Complete your TaskFlow email verification.

                Verification link:
                %s
                """.formatted(buildVerificationLink(rawToken)));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("Failed to send email verification mail.", exception);
        }
    }

    private String buildVerificationLink(String rawToken) {
        if (verifyBaseUrl.contains("{token}")) {
            return UriComponentsBuilder.fromUriString(verifyBaseUrl)
                    .buildAndExpand(Map.of("token", rawToken))
                    .encode()
                    .toUriString();
        }

        return UriComponentsBuilder.fromUriString(verifyBaseUrl)
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }
}
