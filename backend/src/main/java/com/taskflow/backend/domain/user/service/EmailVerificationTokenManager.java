package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.entity.EmailVerificationToken;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenManager {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationTokenGenerator emailVerificationTokenGenerator;
    private final EmailVerificationMailService emailVerificationMailService;

    public void issue(User user) {
        ensureMailServiceReady();
        String rawToken = emailVerificationTokenGenerator.generate();
        EmailVerificationToken token = EmailVerificationToken.create(
                user,
                EmailVerificationTokenHashUtils.hash(rawToken),
                LocalDateTime.now().plusHours(24)
        );
        emailVerificationTokenRepository.save(token);
        emailVerificationMailService.sendVerificationEmail(user.getEmail(), rawToken);
    }

    public void reissue(User user) {
        revokeActiveTokens(user.getId(), null);
        issue(user);
    }

    public void revokeOtherActiveTokens(Long userId, EmailVerificationToken tokenToKeep) {
        revokeActiveTokens(userId, tokenToKeep);
    }

    private void revokeActiveTokens(Long userId, EmailVerificationToken tokenToKeep) {
        LocalDateTime now = LocalDateTime.now();
        emailVerificationTokenRepository.findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(userId)
                .stream()
                .filter(token -> tokenToKeep == null || !token.getId().equals(tokenToKeep.getId()))
                .forEach(token -> token.revoke(now));
    }

    private void ensureMailServiceReady() {
        if (!emailVerificationMailService.isReady()) {
            throw new IllegalStateException("Email verification mail service is not ready.");
        }
    }
}
