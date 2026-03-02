package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;

public interface InvitationEmailService {

    void sendInvitationCreatedEmail(ProjectInvitation invitation);
}

