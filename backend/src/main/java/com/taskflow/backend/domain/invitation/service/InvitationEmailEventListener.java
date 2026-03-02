package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InvitationEmailEventListener {

    private final InvitationEmailService invitationEmailService;

    @Async("invitationEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationCreated(InvitationCreatedEvent event) {
        invitationEmailService.sendInvitationCreatedEmail(event);
    }
}

