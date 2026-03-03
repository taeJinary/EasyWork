package com.taskflow.backend.global.ops;

import com.taskflow.backend.domain.attachment.repository.TaskAttachmentCleanupJobRepository;
import com.taskflow.backend.domain.invitation.repository.InvitationEmailRetryJobRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushRetryJobRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetryQueueMaintenanceServiceTest {

    @Mock
    private InvitationEmailRetryJobRepository invitationEmailRetryJobRepository;

    @Mock
    private NotificationPushRetryJobRepository notificationPushRetryJobRepository;

    @Mock
    private TaskAttachmentCleanupJobRepository taskAttachmentCleanupJobRepository;

    @InjectMocks
    private RetryQueueMaintenanceService retryQueueMaintenanceService;

    @Test
    void maintainCollectsBacklogAndDeletesExpiredHistory() {
        ReflectionTestUtils.setField(retryQueueMaintenanceService, "enabled", true);
        ReflectionTestUtils.setField(retryQueueMaintenanceService, "retentionDays", 7L);
        ReflectionTestUtils.setField(retryQueueMaintenanceService, "pendingWarnThreshold", 5L);
        ReflectionTestUtils.setField(retryQueueMaintenanceService, "deleteBatchSize", 250);

        given(invitationEmailRetryJobRepository.countByCompletedAtIsNull()).willReturn(2L);
        given(notificationPushRetryJobRepository.countByCompletedAtIsNull()).willReturn(3L);
        given(taskAttachmentCleanupJobRepository.countByCompletedAtIsNull()).willReturn(1L);
        given(invitationEmailRetryJobRepository.deleteCompletedHistoryBefore(any(LocalDateTime.class), eq(250)))
                .willReturn(4);
        given(notificationPushRetryJobRepository.deleteCompletedHistoryBefore(any(LocalDateTime.class), eq(250)))
                .willReturn(5);
        given(taskAttachmentCleanupJobRepository.deleteCompletedHistoryBefore(any(LocalDateTime.class), eq(250)))
                .willReturn(6);

        retryQueueMaintenanceService.maintain();

        verify(invitationEmailRetryJobRepository).countByCompletedAtIsNull();
        verify(notificationPushRetryJobRepository).countByCompletedAtIsNull();
        verify(taskAttachmentCleanupJobRepository).countByCompletedAtIsNull();
        verify(invitationEmailRetryJobRepository).deleteCompletedHistoryBefore(any(LocalDateTime.class), eq(250));
        verify(notificationPushRetryJobRepository).deleteCompletedHistoryBefore(any(LocalDateTime.class), eq(250));
        verify(taskAttachmentCleanupJobRepository).deleteCompletedHistoryBefore(any(LocalDateTime.class), eq(250));
    }

    @Test
    void maintainSkipsAllWorkWhenDisabled() {
        ReflectionTestUtils.setField(retryQueueMaintenanceService, "enabled", false);

        retryQueueMaintenanceService.maintain();

        verify(invitationEmailRetryJobRepository, never()).countByCompletedAtIsNull();
        verify(notificationPushRetryJobRepository, never()).countByCompletedAtIsNull();
        verify(taskAttachmentCleanupJobRepository, never()).countByCompletedAtIsNull();
        verify(invitationEmailRetryJobRepository, never())
                .deleteCompletedHistoryBefore(any(LocalDateTime.class), anyInt());
    }
}
