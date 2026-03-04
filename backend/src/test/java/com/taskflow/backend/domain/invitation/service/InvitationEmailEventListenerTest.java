package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import com.taskflow.backend.global.common.enums.ProjectRole;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InvitationEmailEventListenerTest {

    @Test
    void onInvitationCreatedDelegatesToInvitationEmailService() {
        InvitationEmailService invitationEmailService = mock(InvitationEmailService.class);
        InvitationEmailRetryService invitationEmailRetryService = mock(InvitationEmailRetryService.class);
        InvitationEmailEventListener listener = new InvitationEmailEventListener(
                invitationEmailService,
                invitationEmailRetryService
        );
        InvitationCreatedEvent event = new InvitationCreatedEvent(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER
        );

        listener.onInvitationCreated(event);

        verify(invitationEmailService).sendInvitationCreatedEmail(event);
    }

    @Test
    void onInvitationCreatedEnqueuesRetryWhenEmailSendFails() {
        InvitationEmailService invitationEmailService = mock(InvitationEmailService.class);
        InvitationEmailRetryService invitationEmailRetryService = mock(InvitationEmailRetryService.class);
        InvitationEmailEventListener listener = new InvitationEmailEventListener(
                invitationEmailService,
                invitationEmailRetryService
        );
        InvitationCreatedEvent event = new InvitationCreatedEvent(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER
        );

        doThrow(new RuntimeException("smtp down"))
                .when(invitationEmailService)
                .sendInvitationCreatedEmail(event);

        listener.onInvitationCreated(event);

        verify(invitationEmailRetryService).enqueueFailure(event, "smtp down");
    }
}

