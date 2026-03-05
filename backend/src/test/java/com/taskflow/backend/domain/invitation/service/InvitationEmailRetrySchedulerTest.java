package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.global.ops.OperationalMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationEmailRetrySchedulerTest {

    @Mock
    private InvitationEmailRetryService invitationEmailRetryService;

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @InjectMocks
    private InvitationEmailRetryScheduler invitationEmailRetryScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationEmailRetryScheduler, "batchSize", 30);
    }

    @Test
    void retryFailedEmailsCallsServiceWithConfiguredBatchSize() {
        invitationEmailRetryScheduler.retryFailedEmails();

        verify(invitationEmailRetryService).retryPendingEmails(30);
        verify(operationalMetricsService, never()).incrementInvitationEmailRetryExecutionFailure();
    }

    @Test
    void retryFailedEmailsRecordsMetricWhenServiceThrows() {
        willThrow(new RuntimeException("scheduler failure"))
                .given(invitationEmailRetryService)
                .retryPendingEmails(30);

        assertThatCode(() -> invitationEmailRetryScheduler.retryFailedEmails())
                .doesNotThrowAnyException();

        verify(operationalMetricsService).incrementInvitationEmailRetryExecutionFailure();
    }
}
