package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpInvitationEmailService implements InvitationEmailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${app.invitation.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.invitation.email.from:noreply@taskflow.local}")
    private String fromAddress;

    @Value("${app.invitation.email.subject-prefix:[TaskFlow]}")
    private String subjectPrefix;

    @Value("${app.invitation.email.accept-base-url:http://localhost:5173/invitations}")
    private String acceptBaseUrl;

    @Override
    public void sendInvitationCreatedEmail(InvitationCreatedEvent event) {
        if (!emailEnabled) {
            return;
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            return;
        }

        String recipient = event.inviteeEmail();
        if (!StringUtils.hasText(recipient)) {
            return;
        }

        String invitationLink = buildInvitationLink(event.invitationId());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subjectPrefix + " Project invitation");
        message.setText("""
                You were invited to a TaskFlow project.

                Project: %s
                Invited by: %s
                Invitation role: %s

                Accept invitation:
                %s
                """.formatted(
                event.projectName(),
                event.inviterNickname(),
                event.role().name(),
                invitationLink
        ));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("Failed to send invitation email.", exception);
        }
    }

    private String buildInvitationLink(Long invitationId) {
        if (acceptBaseUrl.contains("{invitationId}")) {
            return UriComponentsBuilder.fromUriString(acceptBaseUrl)
                    .buildAndExpand(Map.of("invitationId", invitationId))
                    .encode()
                    .toUriString();
        }

        return UriComponentsBuilder.fromUriString(acceptBaseUrl)
                .pathSegment(String.valueOf(invitationId))
                .build()
                .encode()
                .toUriString();
    }
}
