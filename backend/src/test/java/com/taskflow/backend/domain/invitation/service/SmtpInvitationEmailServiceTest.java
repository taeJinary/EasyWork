package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SmtpInvitationEmailServiceTest {

    private JavaMailSender javaMailSender;
    private SmtpInvitationEmailService smtpInvitationEmailService;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        smtpInvitationEmailService = new SmtpInvitationEmailService(javaMailSender);

        ReflectionTestUtils.setField(smtpInvitationEmailService, "fromAddress", "noreply@taskflow.local");
        ReflectionTestUtils.setField(smtpInvitationEmailService, "subjectPrefix", "[TaskFlow]");
        ReflectionTestUtils.setField(smtpInvitationEmailService, "acceptBaseUrl", "http://localhost:5173/invitations");
    }

    @Test
    void sendInvitationCreatedEmailSkipsWhenDisabled() {
        ReflectionTestUtils.setField(smtpInvitationEmailService, "emailEnabled", false);

        smtpInvitationEmailService.sendInvitationCreatedEmail(pendingInvitation(10L));

        verify(javaMailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void sendInvitationCreatedEmailSendsMailWhenEnabled() {
        ReflectionTestUtils.setField(smtpInvitationEmailService, "emailEnabled", true);

        smtpInvitationEmailService.sendInvitationCreatedEmail(pendingInvitation(10L));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@taskflow.local");
        assertThat(message.getTo()).containsExactly("invitee@example.com");
        assertThat(message.getSubject()).isEqualTo("[TaskFlow] Project invitation");
        assertThat(message.getText()).contains("TaskFlow");
        assertThat(message.getText()).contains("http://localhost:5173/invitations/10");
    }

    private ProjectInvitation pendingInvitation(Long invitationId) {
        User inviter = User.builder()
                .id(1L)
                .email("owner@example.com")
                .nickname("owner")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        User invitee = User.builder()
                .id(2L)
                .email("invitee@example.com")
                .nickname("invitee")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        Project project = Project.builder()
                .id(20L)
                .owner(inviter)
                .name("TaskFlow")
                .description("project")
                .build();

        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviter,
                invitee,
                ProjectRole.MEMBER,
                InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7)
        );
        ReflectionTestUtils.setField(invitation, "id", invitationId);
        return invitation;
    }
}

