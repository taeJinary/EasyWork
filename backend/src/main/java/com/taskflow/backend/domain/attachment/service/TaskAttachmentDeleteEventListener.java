package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.event.TaskAttachmentDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAttachmentDeleteEventListener {

    private final TaskAttachmentStorage taskAttachmentStorage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAttachmentDeleted(TaskAttachmentDeletedEvent event) {
        try {
            taskAttachmentStorage.delete(event.storagePath());
        } catch (Exception exception) {
            log.error(
                    "Failed to delete attachment file after commit. attachmentId={}, storagePath={}",
                    event.attachmentId(),
                    event.storagePath(),
                    exception
            );
        }
    }
}
