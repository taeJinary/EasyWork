package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import com.taskflow.backend.global.common.enums.ProjectRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpInvitationEmailServiceTest {

    private JavaMailSender javaMailSender;
    private ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private SmtpInvitationEmailService smtpInvitationEmailService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        javaMailSenderProvider = mock(ObjectProvider.class);
        smtpInvitationEmailService = new SmtpInvitationEmailService(javaMailSenderProvider);

        ReflectionTestUtils.setField(smtpInvitationEmailService, "fromAddress", "noreply@taskflow.local");
        ReflectionTestUtils.setField(smtpInvitationEmailService, "subjectPrefix", "[TaskFlow]");
        ReflectionTestUtils.setField(smtpInvitationEmailService, "acceptBaseUrl", "http://localhost:5173/invitations");
    }

    @Test
    void sendInvitationCreatedEmailSkipsWhenDisabled() {
        ReflectionTestUtils.setField(smtpInvitationEmailService, "emailEnabled", false);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        smtpInvitationEmailService.sendInvitationCreatedEmail(invitationCreatedEvent(10L));

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendInvitationCreatedEmailSkipsWhenMailSenderIsMissing() {
        ReflectionTestUtils.setField(smtpInvitationEmailService, "emailEnabled", true);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(null);

        smtpInvitationEmailService.sendInvitationCreatedEmail(invitationCreatedEvent(10L));

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendInvitationCreatedEmailSendsMailWhenEnabled() {
        ReflectionTestUtils.setField(smtpInvitationEmailService, "emailEnabled", true);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        smtpInvitationEmailService.sendInvitationCreatedEmail(invitationCreatedEvent(10L));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@taskflow.local");
        assertThat(message.getTo()).containsExactly("invitee@example.com");
        assertThat(message.getSubject()).isEqualTo("[TaskFlow] Project invitation");
        assertThat(message.getText()).contains("TaskFlow");
        assertThat(message.getText()).contains("http://localhost:5173/invitations/10");
    }

    @Test
    void sendInvitationCreatedEmailBuildsSafeLinkWhenBaseUrlHasQueryAndFragment() {
        ReflectionTestUtils.setField(smtpInvitationEmailService, "emailEnabled", true);
        ReflectionTestUtils.setField(
                smtpInvitationEmailService,
                "acceptBaseUrl",
                "https://taskflow.example.com/invitations?utm=mail#section"
        );
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        smtpInvitationEmailService.sendInvitationCreatedEmail(invitationCreatedEvent(99L));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        String messageBody = messageCaptor.getValue().getText();
        assertThat(messageBody).contains("https://taskflow.example.com/invitations/99?utm=mail#section");
    }

    private InvitationCreatedEvent invitationCreatedEvent(Long invitationId) {
        return new InvitationCreatedEvent(
                invitationId,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER
        );
    }
}

