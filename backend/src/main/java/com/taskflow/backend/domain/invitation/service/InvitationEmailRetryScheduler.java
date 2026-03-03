package com.taskflow.backend.domain.invitation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationEmailRetryScheduler {

    private final InvitationEmailRetryService invitationEmailRetryService;

    @Value("${app.invitation.email.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.invitation.email.retry.interval-ms:60000}")
    public void retryFailedEmails() {
        invitationEmailRetryService.retryPendingEmails(batchSize);
    }
}
