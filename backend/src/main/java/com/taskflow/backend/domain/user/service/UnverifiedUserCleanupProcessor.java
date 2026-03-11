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
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupProcessor {

    static final String LOCAL_PROVIDER = "LOCAL";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "login:lock:";
    private static final String EMAIL_VERIFICATION_RESEND_COOLDOWN_KEY_PREFIX = "email-verification:resend:cooldown:";
    private static final String EMAIL_VERIFICATION_RESEND_COUNT_KEY_PREFIX = "email-verification:resend:count:";

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationRetryJobRepository emailVerificationRetryJobRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPushTokenRepository notificationPushTokenRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RedisService redisService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CleanupResult processCandidate(Long userId) {
        User candidate = userRepository
                .findByIdAndProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNull(userId, LOCAL_PROVIDER)
                .orElse(null);

        if (candidate == null) {
            return CleanupResult.SKIPPED;
        }

        if (hasCollaborationState(candidate.getId())) {
            return CleanupResult.SKIPPED;
        }

        deleteDependentRows(candidate);
        int deleted = userRepository.deleteCleanupCandidate(candidate.getId(), LOCAL_PROVIDER);
        if (deleted == 0) {
            throw new ConcurrentCleanupSkipException(candidate.getId());
        }
        return CleanupResult.DELETED;
    }

    private boolean hasCollaborationState(Long userId) {
        return workspaceRepository.existsByOwnerIdAndDeletedAtIsNull(userId)
                || projectRepository.existsByOwnerIdAndDeletedAtIsNull(userId)
                || workspaceMemberRepository.existsByUserId(userId)
                || projectMemberRepository.existsByUserId(userId);
    }

    private void deleteDependentRows(User user) {
        Long userId = user.getId();

        projectInvitationRepository.deleteAllByInviteeIdOrInviterId(userId, userId);
        workspaceInvitationRepository.deleteAllByInviteeIdOrInviterId(userId, userId);
        notificationRepository.deleteAllByUserId(userId);
        notificationPushTokenRepository.deleteAllByUserId(userId);
        passwordHistoryRepository.deleteAllByUserId(userId);
        emailVerificationTokenRepository.deleteAllByUserId(userId);
        emailVerificationRetryJobRepository.deleteAllByUserId(userId);

        redisService.deleteByPattern(refreshTokenKeyPattern(userId));
        redisService.delete(emailVerificationResendCooldownKey(userId));
        redisService.delete(emailVerificationResendCountKey(userId));
        redisService.delete(loginFailKey(user.getEmail()));
        redisService.delete(loginLockKey(user.getEmail()));
    }

    private String refreshTokenKeyPattern(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":*";
    }

    private String emailVerificationResendCooldownKey(Long userId) {
        return EMAIL_VERIFICATION_RESEND_COOLDOWN_KEY_PREFIX + userId;
    }

    private String emailVerificationResendCountKey(Long userId) {
        return EMAIL_VERIFICATION_RESEND_COUNT_KEY_PREFIX + userId;
    }

    private String loginFailKey(String email) {
        return LOGIN_FAIL_KEY_PREFIX + normalizeEmail(email);
    }

    private String loginLockKey(String email) {
        return LOGIN_LOCK_KEY_PREFIX + normalizeEmail(email);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "unknown";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public enum CleanupResult {
        DELETED,
        SKIPPED
    }

    public static final class ConcurrentCleanupSkipException extends RuntimeException {

        public ConcurrentCleanupSkipException(Long userId) {
            super("Skipped cleanup because candidate changed before delete. userId=" + userId);
        }
    }
}
