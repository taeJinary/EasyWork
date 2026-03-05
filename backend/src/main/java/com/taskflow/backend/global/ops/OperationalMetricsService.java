package com.taskflow.backend.global.ops;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OperationalMetricsService {

    private final Counter loginFailureCounter;
    private final Counter refreshReissueFailureCounter;
    private final Counter websocketConnectFailureCounter;
    private final Counter fileUploadFailureCounter;

    private final AtomicLong invitationRetryBacklog = new AtomicLong(0L);
    private final AtomicLong notificationPushRetryBacklog = new AtomicLong(0L);
    private final AtomicLong attachmentCleanupRetryBacklog = new AtomicLong(0L);
    private final AtomicLong totalRetryBacklog = new AtomicLong(0L);

    public OperationalMetricsService(MeterRegistry meterRegistry) {
        this.loginFailureCounter = Counter.builder("taskflow.auth.login.failure.total")
                .description("Total number of failed login attempts")
                .register(meterRegistry);
        this.refreshReissueFailureCounter = Counter.builder("taskflow.auth.refresh.reissue.failure.total")
                .description("Total number of failed refresh token reissue attempts")
                .register(meterRegistry);
        this.websocketConnectFailureCounter = Counter.builder("taskflow.websocket.connect.failure.total")
                .description("Total number of failed websocket CONNECT attempts")
                .register(meterRegistry);
        this.fileUploadFailureCounter = Counter.builder("taskflow.attachment.upload.failure.total")
                .description("Total number of failed task attachment uploads")
                .register(meterRegistry);

        Gauge.builder("taskflow.retry.queue.backlog.invitation", invitationRetryBacklog, AtomicLong::get)
                .description("Pending invitation email retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.push", notificationPushRetryBacklog, AtomicLong::get)
                .description("Pending push retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.attachment", attachmentCleanupRetryBacklog, AtomicLong::get)
                .description("Pending attachment cleanup retry jobs")
                .register(meterRegistry);
        Gauge.builder("taskflow.retry.queue.backlog.total", totalRetryBacklog, AtomicLong::get)
                .description("Total pending retry jobs across all retry queues")
                .register(meterRegistry);
    }

    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }

    public void incrementRefreshReissueFailure() {
        refreshReissueFailureCounter.increment();
    }

    public void incrementWebSocketConnectFailure() {
        websocketConnectFailureCounter.increment();
    }

    public void incrementFileUploadFailure() {
        fileUploadFailureCounter.increment();
    }

    public void updateRetryQueueBacklog(long invitationPending, long notificationPending, long attachmentPending) {
        invitationRetryBacklog.set(Math.max(invitationPending, 0L));
        notificationPushRetryBacklog.set(Math.max(notificationPending, 0L));
        attachmentCleanupRetryBacklog.set(Math.max(attachmentPending, 0L));
        totalRetryBacklog.set(Math.max(invitationPending + notificationPending + attachmentPending, 0L));
    }
}
