package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;

public interface InvitationEmailService {

    void sendInvitationCreatedEmail(InvitationCreatedEvent event);
}

