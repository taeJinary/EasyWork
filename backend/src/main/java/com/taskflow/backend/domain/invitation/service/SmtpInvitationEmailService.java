package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    public void sendInvitationCreatedEmail(ProjectInvitation invitation) {
        if (!emailEnabled) {
            return;
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            return;
        }

        String recipient = invitation.getInvitee().getEmail();
        if (!StringUtils.hasText(recipient)) {
            return;
        }

        String projectName = invitation.getProject().getName();
        String inviterNickname = invitation.getInviter().getNickname();
        String invitationLink = buildInvitationLink(invitation.getId());

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
                projectName,
                inviterNickname,
                invitation.getRole().name(),
                invitationLink
        ));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            log.warn("Failed to send invitation email. invitationId={}, to={}", invitation.getId(), recipient, exception);
        }
    }

    private String buildInvitationLink(Long invitationId) {
        if (acceptBaseUrl.contains("{invitationId}")) {
            return acceptBaseUrl.replace("{invitationId}", String.valueOf(invitationId));
        }

        String normalizedBase = acceptBaseUrl.endsWith("/")
                ? acceptBaseUrl.substring(0, acceptBaseUrl.length() - 1)
                : acceptBaseUrl;
        return normalizedBase + "/" + invitationId;
    }
}
