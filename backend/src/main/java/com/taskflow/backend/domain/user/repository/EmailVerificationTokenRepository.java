package com.taskflow.backend.domain.user.repository;

import com.taskflow.backend.domain.user.entity.EmailVerificationToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(Long userId);
}
