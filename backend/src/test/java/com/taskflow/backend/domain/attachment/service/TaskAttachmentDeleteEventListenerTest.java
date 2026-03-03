package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.event.TaskAttachmentDeletedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TaskAttachmentDeleteEventListenerTest {

    @Test
    void onTaskAttachmentDeletedDelegatesToStorageDelete() {
        TaskAttachmentStorage taskAttachmentStorage = mock(TaskAttachmentStorage.class);
        TaskAttachmentCleanupRetryService cleanupRetryService = mock(TaskAttachmentCleanupRetryService.class);
        TaskAttachmentDeleteEventListener listener =
                new TaskAttachmentDeleteEventListener(taskAttachmentStorage, cleanupRetryService);
        TaskAttachmentDeletedEvent event = new TaskAttachmentDeletedEvent(2000L, "task-attachments/10/abc-report.pdf");

        listener.onTaskAttachmentDeleted(event);

        verify(taskAttachmentStorage).delete("task-attachments/10/abc-report.pdf");
        verify(cleanupRetryService, never()).enqueueDeleteFailure(2000L, "task-attachments/10/abc-report.pdf");
    }

    @Test
    void onTaskAttachmentDeletedEnqueuesRetryWhenStorageDeleteFails() {
        TaskAttachmentStorage taskAttachmentStorage = mock(TaskAttachmentStorage.class);
        TaskAttachmentCleanupRetryService cleanupRetryService = mock(TaskAttachmentCleanupRetryService.class);
        TaskAttachmentDeleteEventListener listener =
                new TaskAttachmentDeleteEventListener(taskAttachmentStorage, cleanupRetryService);
        TaskAttachmentDeletedEvent event = new TaskAttachmentDeletedEvent(2000L, "task-attachments/10/abc-report.pdf");
        doThrow(new RuntimeException("storage fail"))
                .when(taskAttachmentStorage)
                .delete("task-attachments/10/abc-report.pdf");

        listener.onTaskAttachmentDeleted(event);

        verify(cleanupRetryService).enqueueDeleteFailure(2000L, "task-attachments/10/abc-report.pdf");
    }
}
