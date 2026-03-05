package com.taskflow.backend.domain.attachment.service;

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
class TaskAttachmentCleanupRetrySchedulerTest {

    @Mock
    private TaskAttachmentCleanupRetryService cleanupRetryService;

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @InjectMocks
    private TaskAttachmentCleanupRetryScheduler taskAttachmentCleanupRetryScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taskAttachmentCleanupRetryScheduler, "batchSize", 40);
    }

    @Test
    void retryFailedDeletesCallsServiceWithConfiguredBatchSize() {
        taskAttachmentCleanupRetryScheduler.retryFailedDeletes();

        verify(cleanupRetryService).retryPendingDeletes(40);
        verify(operationalMetricsService, never()).incrementAttachmentCleanupRetryExecutionFailure();
    }

    @Test
    void retryFailedDeletesRecordsMetricWhenServiceThrows() {
        willThrow(new RuntimeException("scheduler failure"))
                .given(cleanupRetryService)
                .retryPendingDeletes(40);

        assertThatCode(() -> taskAttachmentCleanupRetryScheduler.retryFailedDeletes())
                .doesNotThrowAnyException();

        verify(operationalMetricsService).incrementAttachmentCleanupRetryExecutionFailure();
    }
}
