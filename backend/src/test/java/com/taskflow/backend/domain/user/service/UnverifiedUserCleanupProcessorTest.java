package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.invitation.repository.WorkspaceInvitationRepository;
import com.taskflow.backend.domain.notification.repository.NotificationPushTokenRepository;
import com.taskflow.backend.domain.notification.repository.NotificationRepository;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationRetryJobRepository;
import com.taskflow.backend.domain.user.repository.EmailVerificationTokenRepository;
import com.taskflow.backend.domain.user.repository.PasswordHistoryRepository;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.infra.redis.RedisService;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnverifiedUserCleanupProcessorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailVerificationRetryJobRepository emailVerificationRetryJobRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPushTokenRepository notificationPushTokenRepository;

    @Mock
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private UnverifiedUserCleanupProcessor unverifiedUserCleanupProcessor;

    @Test
    void processCandidateThrowsAndRollsBackWhenDeleteConditionNoLongerMatches() {
        User candidate = User.builder()
                .id(11L)
                .email("cleanup@example.com")
                .password("encoded")
                .nickname("cleanup-user")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByIdAndProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNull(
                candidate.getId(),
                UnverifiedUserCleanupProcessor.LOCAL_PROVIDER
        )).thenReturn(Optional.of(candidate));
        when(workspaceRepository.existsByOwnerIdAndDeletedAtIsNull(candidate.getId())).thenReturn(false);
        when(projectRepository.existsByOwnerIdAndDeletedAtIsNull(candidate.getId())).thenReturn(false);
        when(workspaceMemberRepository.existsByUserId(candidate.getId())).thenReturn(false);
        when(projectMemberRepository.existsByUserId(candidate.getId())).thenReturn(false);
        when(userRepository.deleteCleanupCandidate(candidate.getId(), UnverifiedUserCleanupProcessor.LOCAL_PROVIDER))
                .thenReturn(0);

        assertThatThrownBy(() -> unverifiedUserCleanupProcessor.processCandidate(candidate.getId()))
                .isInstanceOf(UnverifiedUserCleanupProcessor.ConcurrentCleanupSkipException.class);

        verify(projectInvitationRepository).deleteAllByInviteeIdOrInviterId(candidate.getId(), candidate.getId());
        verify(workspaceInvitationRepository).deleteAllByInviteeIdOrInviterId(candidate.getId(), candidate.getId());
        verify(notificationRepository).deleteAllByUserId(candidate.getId());
        verify(notificationPushTokenRepository).deleteAllByUserId(candidate.getId());
        verify(passwordHistoryRepository).deleteAllByUserId(candidate.getId());
        verify(emailVerificationTokenRepository).deleteAllByUserId(candidate.getId());
        verify(emailVerificationRetryJobRepository).deleteAllByUserId(candidate.getId());
        verify(redisService).deleteByPattern("refresh:" + candidate.getId() + ":*");
    }

    @Test
    void processCandidateSkipsWithoutDeletingDependenciesWhenCandidateAlreadyVerified() {
        when(userRepository.findByIdAndProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNull(
                99L,
                UnverifiedUserCleanupProcessor.LOCAL_PROVIDER
        )).thenReturn(Optional.empty());

        UnverifiedUserCleanupProcessor.CleanupResult result = unverifiedUserCleanupProcessor.processCandidate(99L);

        verify(projectInvitationRepository, never()).deleteAllByInviteeIdOrInviterId(eq(99L), eq(99L));
        verify(redisService, never()).delete(anyString());
        verify(redisService, never()).deleteByPattern(anyString());
        verify(userRepository, never()).deleteCleanupCandidate(eq(99L), anyString());
        org.assertj.core.api.Assertions.assertThat(result)
                .isEqualTo(UnverifiedUserCleanupProcessor.CleanupResult.SKIPPED);
    }
}
