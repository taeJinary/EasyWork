package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.event.TaskAttachmentDeletedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TaskAttachmentDeleteEventListenerTest {

    @Test
    void onTaskAttachmentDeletedDelegatesToStorageDelete() {
        TaskAttachmentStorage taskAttachmentStorage = mock(TaskAttachmentStorage.class);
        TaskAttachmentDeleteEventListener listener = new TaskAttachmentDeleteEventListener(taskAttachmentStorage);
        TaskAttachmentDeletedEvent event = new TaskAttachmentDeletedEvent(2000L, "task-attachments/10/abc-report.pdf");

        listener.onTaskAttachmentDeleted(event);

        verify(taskAttachmentStorage).delete("task-attachments/10/abc-report.pdf");
    }
}
