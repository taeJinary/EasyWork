package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.entity.EmailVerificationRetryJob;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationRetryJobRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationRetryServiceTest {

    @Mock
    private EmailVerificationRetryJobRepository emailVerificationRetryJobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenManager emailVerificationTokenManager;

    @Mock
    private OperationalMetricsService operationalMetricsService;

    @InjectMocks
    private EmailVerificationRetryService emailVerificationRetryService;

    @Test
    void enqueueFailureCreatesPendingJobOnce() {
        when(emailVerificationRetryJobRepository.existsByUserIdAndCompletedAtIsNull(1L)).thenReturn(false);

        emailVerificationRetryService.enqueueFailure(1L, "smtp down");

        verify(emailVerificationRetryJobRepository).save(any(EmailVerificationRetryJob.class));
        verify(operationalMetricsService).incrementEmailVerificationRetryEnqueued();
    }

    @Test
    void retryPendingEmailsCompletesJobWhenReissueSucceeds() {
        EmailVerificationRetryJob job = EmailVerificationRetryJob.createPending(1L, LocalDateTime.now().minusSeconds(1), "smtp down");
        User user = localUser();

        when(emailVerificationRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        emailVerificationRetryService.retryPendingEmails(10);

        verify(emailVerificationTokenManager).reissue(user);
        assertThat(job.getCompletedAt()).isNotNull();
        verify(operationalMetricsService).incrementEmailVerificationRetryCompleted();
    }

    @Test
    void retryPendingEmailsReschedulesJobWhenReissueFails() {
        EmailVerificationRetryJob job = EmailVerificationRetryJob.createPending(1L, LocalDateTime.now().minusSeconds(1), "smtp down");
        User user = localUser();
        ReflectionTestUtils.setField(emailVerificationRetryService, "retryDelaySeconds", 60L);
        ReflectionTestUtils.setField(emailVerificationRetryService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(emailVerificationRetryService, "maxRetryDelaySeconds", 3600L);

        when(emailVerificationRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(emailVerificationTokenManager).reissue(user);

        emailVerificationRetryService.retryPendingEmails(10);

        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getNextRetryAt()).isAfter(LocalDateTime.now().plusSeconds(30));
        verify(operationalMetricsService).incrementEmailVerificationRetryRescheduled();
    }

    @Test
    void retryPendingEmailsCompletesJobWhenUserAlreadyVerified() {
        EmailVerificationRetryJob job = EmailVerificationRetryJob.createPending(1L, LocalDateTime.now().minusSeconds(1), "smtp down");
        User user = localUser();
        user.markEmailVerified(LocalDateTime.now());

        when(emailVerificationRetryJobRepository.findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        emailVerificationRetryService.retryPendingEmails(10);

        assertThat(job.getCompletedAt()).isNotNull();
        verify(emailVerificationTokenManager, never()).reissue(any(User.class));
        verify(operationalMetricsService).incrementEmailVerificationRetryCompleted();
    }

    private User localUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .nickname("user")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
