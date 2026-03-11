package com.taskflow.backend.domain.user.repository;

import com.taskflow.backend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    List<User> findByProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNullAndCreatedAtBeforeOrderByIdAsc(
            String provider,
            LocalDateTime cutoff,
            Pageable pageable
    );
}
