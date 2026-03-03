package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.InvitationEmailRetryJob;
import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import com.taskflow.backend.domain.invitation.repository.InvitationEmailRetryJobRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationEmailRetryServiceTest {

    @Mock
    private InvitationEmailRetryJobRepository invitationEmailRetryJobRepository;

    @Mock
    private InvitationEmailService invitationEmailService;

    @InjectMocks
    private InvitationEmailRetryService invitationEmailRetryService;

    @Test
    void enqueueFailureSavesPendingJobWhenNoOpenJobExists() {
        InvitationCreatedEvent event = invitationCreatedEvent(10L);
        given(invitationEmailRetryJobRepository.existsByInvitationIdAndCompletedAtIsNull(10L))
                .willReturn(false);

        invitationEmailRetryService.enqueueFailure(event, "smtp down");

        ArgumentCaptor<InvitationEmailRetryJob> captor = ArgumentCaptor.forClass(InvitationEmailRetryJob.class);
        verify(invitationEmailRetryJobRepository).save(captor.capture());
        InvitationEmailRetryJob saved = captor.getValue();
        assertThat(saved.getInvitationId()).isEqualTo(10L);
        assertThat(saved.getInviteeEmail()).isEqualTo("invitee@example.com");
        assertThat(saved.getProjectName()).isEqualTo("TaskFlow");
        assertThat(saved.getInviterNickname()).isEqualTo("owner");
        assertThat(saved.getRole()).isEqualTo(ProjectRole.MEMBER);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getNextRetryAt()).isNotNull();
        assertThat(saved.getLastErrorMessage()).contains("smtp down");
    }

    @Test
    void enqueueFailureSkipsWhenOpenJobAlreadyExists() {
        InvitationCreatedEvent event = invitationCreatedEvent(10L);
        given(invitationEmailRetryJobRepository.existsByInvitationIdAndCompletedAtIsNull(10L))
                .willReturn(true);

        invitationEmailRetryService.enqueueFailure(event, "smtp down");

        verify(invitationEmailRetryJobRepository, never()).save(any(InvitationEmailRetryJob.class));
    }

    @Test
    void retryPendingEmailsMarksJobCompletedWhenSendSucceeds() {
        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER,
                LocalDateTime.now().minusMinutes(1),
                "smtp down"
        );
        given(invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));

        invitationEmailRetryService.retryPendingEmails(50);

        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getRetryCount()).isEqualTo(0);
        verify(invitationEmailService).sendInvitationCreatedEmail(any(InvitationCreatedEvent.class));
        verify(invitationEmailRetryJobRepository).save(job);
    }

    @Test
    void retryPendingEmailsReschedulesWhenSendFails() {
        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER,
                LocalDateTime.now().minusMinutes(1),
                "smtp down"
        );
        given(invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        willThrow(new RuntimeException("temporary smtp failure"))
                .given(invitationEmailService)
                .sendInvitationCreatedEmail(any(InvitationCreatedEvent.class));

        invitationEmailRetryService.retryPendingEmails(50);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getLastErrorMessage()).contains("temporary smtp failure");
        assertThat(job.getNextRetryAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        verify(invitationEmailRetryJobRepository).save(job);
    }

    private InvitationCreatedEvent invitationCreatedEvent(Long invitationId) {
        return new InvitationCreatedEvent(
                invitationId,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER
        );
    }
}
