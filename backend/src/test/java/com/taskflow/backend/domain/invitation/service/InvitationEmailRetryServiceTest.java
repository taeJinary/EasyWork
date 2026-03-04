package com.taskflow.backend.domain.invitation.service;

import com.taskflow.backend.domain.invitation.entity.InvitationEmailRetryJob;
import com.taskflow.backend.domain.invitation.entity.ProjectInvitation;
import com.taskflow.backend.domain.invitation.event.InvitationCreatedEvent;
import com.taskflow.backend.domain.invitation.repository.InvitationEmailRetryJobRepository;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @InjectMocks
    private InvitationEmailRetryService invitationEmailRetryService;

    @BeforeEach
    void setUpDefaultRetryPolicy() {
        ReflectionTestUtils.setField(invitationEmailRetryService, "retryDelaySeconds", 300L);
        ReflectionTestUtils.setField(invitationEmailRetryService, "maxRetryAttempts", 10);
        ReflectionTestUtils.setField(invitationEmailRetryService, "maxRetryDelaySeconds", 3600L);
    }

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
        given(projectInvitationRepository.findById(10L))
                .willReturn(Optional.of(invitation(10L, InvitationStatus.PENDING, LocalDateTime.now().plusDays(1))));

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
        given(projectInvitationRepository.findById(10L))
                .willReturn(Optional.of(invitation(10L, InvitationStatus.PENDING, LocalDateTime.now().plusDays(1))));
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

    @Test
    void retryPendingEmailsSkipsWhenInvitationAlreadyProcessed() {
        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER,
                LocalDateTime.now().minusMinutes(1),
                "smtp down"
        );
        ProjectInvitation invitation = invitation(10L, InvitationStatus.ACCEPTED, LocalDateTime.now().plusDays(1));

        given(invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        invitationEmailRetryService.retryPendingEmails(50);

        assertThat(job.getCompletedAt()).isNotNull();
        verify(invitationEmailService, never()).sendInvitationCreatedEmail(any(InvitationCreatedEvent.class));
        verify(invitationEmailRetryJobRepository).save(job);
    }

    @Test
    void retryPendingEmailsSkipsWhenInvitationExpired() {
        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER,
                LocalDateTime.now().minusMinutes(1),
                "smtp down"
        );
        ProjectInvitation invitation = invitation(10L, InvitationStatus.PENDING, LocalDateTime.now().minusMinutes(5));

        given(invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(projectInvitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        invitationEmailRetryService.retryPendingEmails(50);

        assertThat(job.getCompletedAt()).isNotNull();
        verify(invitationEmailService, never()).sendInvitationCreatedEmail(any(InvitationCreatedEvent.class));
        verify(invitationEmailRetryJobRepository).save(job);
    }

    @Test
    void retryPendingEmailsMarksCompletedWhenMaxRetryAttemptsReached() {
        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER,
                LocalDateTime.now().minusMinutes(1),
                "smtp down"
        );
        job.markFailed("first failure", LocalDateTime.now().minusSeconds(20));
        job.markFailed("second failure", LocalDateTime.now().minusSeconds(10));

        ReflectionTestUtils.setField(invitationEmailRetryService, "maxRetryAttempts", 3);
        given(invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(projectInvitationRepository.findById(10L))
                .willReturn(Optional.of(invitation(10L, InvitationStatus.PENDING, LocalDateTime.now().plusDays(1))));
        willThrow(new RuntimeException("smtp down again"))
                .given(invitationEmailService)
                .sendInvitationCreatedEmail(any(InvitationCreatedEvent.class));

        invitationEmailRetryService.retryPendingEmails(50);

        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getRetryCount()).isEqualTo(3);
        assertThat(job.getLastErrorMessage()).contains("smtp down again");
        verify(invitationEmailRetryJobRepository).save(job);
    }

    @Test
    void retryPendingEmailsUsesExponentialBackoffDelay() {
        InvitationEmailRetryJob job = InvitationEmailRetryJob.createPending(
                10L,
                "invitee@example.com",
                "TaskFlow",
                "owner",
                ProjectRole.MEMBER,
                LocalDateTime.now().minusMinutes(1),
                "smtp down"
        );
        job.markFailed("first failure", LocalDateTime.now().minusSeconds(20));
        job.markFailed("second failure", LocalDateTime.now().minusSeconds(10));

        ReflectionTestUtils.setField(invitationEmailRetryService, "retryDelaySeconds", 30L);
        ReflectionTestUtils.setField(invitationEmailRetryService, "maxRetryAttempts", 10);
        ReflectionTestUtils.setField(invitationEmailRetryService, "maxRetryDelaySeconds", 600L);
        given(invitationEmailRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).willReturn(List.of(job));
        given(projectInvitationRepository.findById(10L))
                .willReturn(Optional.of(invitation(10L, InvitationStatus.PENDING, LocalDateTime.now().plusDays(1))));
        willThrow(new RuntimeException("smtp down again"))
                .given(invitationEmailService)
                .sendInvitationCreatedEmail(any(InvitationCreatedEvent.class));
        LocalDateTime startedAt = LocalDateTime.now();

        invitationEmailRetryService.retryPendingEmails(50);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(3);
        assertThat(job.getNextRetryAt()).isAfter(startedAt.plusSeconds(110));
        assertThat(job.getNextRetryAt()).isBefore(startedAt.plusSeconds(130));
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

    private ProjectInvitation invitation(Long invitationId, InvitationStatus status, LocalDateTime expiresAt) {
        User inviter = activeUser(1L, "owner@example.com", "owner");
        User invitee = activeUser(2L, "invitee@example.com", "invitee");
        Project project = Project.builder()
                .id(100L)
                .owner(inviter)
                .name("TaskFlow")
                .description("project")
                .build();

        ProjectInvitation invitation = ProjectInvitation.create(
                project,
                inviter,
                invitee,
                ProjectRole.MEMBER,
                status,
                expiresAt
        );

        org.springframework.test.util.ReflectionTestUtils.setField(invitation, "id", invitationId);
        return invitation;
    }

    private User activeUser(Long id, String email, String nickname) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .nickname(nickname)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
