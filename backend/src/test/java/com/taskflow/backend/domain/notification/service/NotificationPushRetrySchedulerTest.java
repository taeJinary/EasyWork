package com.taskflow.backend.domain.notification.service;

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
class NotificationPushRetrySchedulerTest {

    @Mock
    private NotificationPushRetryService notificationPushRetryService;

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @InjectMocks
    private NotificationPushRetryScheduler notificationPushRetryScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationPushRetryScheduler, "batchSize", 25);
    }

    @Test
    void retryFailedPushesCallsServiceWithConfiguredBatchSize() {
        notificationPushRetryScheduler.retryFailedPushes();

        verify(notificationPushRetryService).retryPendingPushes(25);
        verify(operationalMetricsService, never()).incrementNotificationPushRetryExecutionFailure();
    }

    @Test
    void retryFailedPushesRecordsMetricWhenServiceThrows() {
        willThrow(new RuntimeException("scheduler failure"))
                .given(notificationPushRetryService)
                .retryPendingPushes(25);

        assertThatCode(() -> notificationPushRetryScheduler.retryFailedPushes())
                .doesNotThrowAnyException();

        verify(operationalMetricsService).incrementNotificationPushRetryExecutionFailure();
    }
}
