package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationEmailEventListener {

    private final InvitationEmailService invitationEmailService;
    private final InvitationEmailRetryService invitationEmailRetryService;

    @Async("invitationEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationCreated(InvitationCreatedEvent event) {
        try {
            invitationEmailService.sendInvitationCreatedEmail(event);
        } catch (Exception exception) {
            invitationEmailRetryService.enqueueFailure(event, exception.getMessage());
            log.error("Failed to process invitation email event. invitationId={}", event.invitationId(), exception);
        }
    }
}
