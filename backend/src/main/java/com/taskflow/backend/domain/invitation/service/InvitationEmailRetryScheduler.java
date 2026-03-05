package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.global.ops.OperationalMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationEmailRetryScheduler {

    private final InvitationEmailRetryService invitationEmailRetryService;
    private final OperationalMetricsService operationalMetricsService;

    @Value("${app.invitation.email.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.invitation.email.retry.interval-ms:60000}")
    public void retryFailedEmails() {
        try {
            invitationEmailRetryService.retryPendingEmails(batchSize);
        } catch (RuntimeException exception) {
            operationalMetricsService.incrementInvitationEmailRetryExecutionFailure();
            log.error("Failed to execute invitation email retry scheduler. batchSize={}", batchSize, exception);
        }
    }
}
