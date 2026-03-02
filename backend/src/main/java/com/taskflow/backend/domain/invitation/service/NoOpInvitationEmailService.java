package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(InvitationEmailService.class)
public class NoOpInvitationEmailService implements InvitationEmailService {

    @Override
    public void sendInvitationCreatedEmail(ProjectInvitation invitation) {
        // no-op fallback for environments where mail sender is not configured
    }
}

