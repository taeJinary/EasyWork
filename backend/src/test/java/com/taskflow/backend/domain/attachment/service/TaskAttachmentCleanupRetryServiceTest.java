package com.taskflow.backend.domain.attachment.service;

import com.taskflow.backend.domain.attachment.entity.TaskAttachmentCleanupJob;
import com.taskflow.backend.domain.attachment.repository.TaskAttachmentCleanupJobRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentCleanupRetryServiceTest {

    @Mock
    private TaskAttachmentCleanupJobRepository cleanupJobRepository;

    @Mock
    private TaskAttachmentStorage taskAttachmentStorage;

    @InjectMocks
    private TaskAttachmentCleanupRetryService cleanupRetryService;

    @Test
    void enqueueDeleteFailureSavesPendingJobWhenNoOpenJob() {
        given(cleanupJobRepository.existsByStoragePathAndCompletedAtIsNull("task-attachments/10/abc-report.pdf"))
                .willReturn(false);

        cleanupRetryService.enqueueDeleteFailure(2000L, "task-attachments/10/abc-report.pdf");

        ArgumentCaptor<TaskAttachmentCleanupJob> captor = ArgumentCaptor.forClass(TaskAttachmentCleanupJob.class);
        verify(cleanupJobRepository).save(captor.capture());
        TaskAttachmentCleanupJob saved = captor.getValue();
        assertThat(saved.getAttachmentId()).isEqualTo(2000L);
        assertThat(saved.getStoragePath()).isEqualTo("task-attachments/10/abc-report.pdf");
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getNextRetryAt()).isNotNull();
    }

    @Test
    void enqueueDeleteFailureDoesNotSaveDuplicateOpenJob() {
        given(cleanupJobRepository.existsByStoragePathAndCompletedAtIsNull("task-attachments/10/abc-report.pdf"))
                .willReturn(true);

        cleanupRetryService.enqueueDeleteFailure(2000L, "task-attachments/10/abc-report.pdf");

        verify(cleanupJobRepository, never()).save(any(TaskAttachmentCleanupJob.class));
    }

    @Test
    void retryPendingDeletesMarksJobCompletedWhenDeleteSucceeds() {
        TaskAttachmentCleanupJob job = TaskAttachmentCleanupJob.createPending(
                2000L,
                "task-attachments/10/abc-report.pdf",
                LocalDateTime.now().minusMinutes(1)
        );
        given(cleanupJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));

        cleanupRetryService.retryPendingDeletes(50);

        verify(taskAttachmentStorage).delete("task-attachments/10/abc-report.pdf");
        assertThat(job.getCompletedAt()).isNotNull();
        verify(cleanupJobRepository).save(job);
    }

    @Test
    void retryPendingDeletesReschedulesWhenDeleteFails() {
        TaskAttachmentCleanupJob job = TaskAttachmentCleanupJob.createPending(
                2000L,
                "task-attachments/10/abc-report.pdf",
                LocalDateTime.now().minusMinutes(1)
        );
        given(cleanupJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        willThrow(new RuntimeException("storage down"))
                .given(taskAttachmentStorage)
                .delete(anyString());

        cleanupRetryService.retryPendingDeletes(50);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getLastErrorMessage()).contains("storage down");
        assertThat(job.getNextRetryAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        verify(cleanupJobRepository).save(job);
    }
}
