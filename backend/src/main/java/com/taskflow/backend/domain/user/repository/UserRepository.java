package com.taskflow.backend.domain.user.repository;

import com.taskflow.backend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    List<User> findByProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNullAndCreatedAtBeforeOrderByIdAsc(
            String provider,
            LocalDateTime cutoff,
            Pageable pageable
    );

    List<User> findByProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNullAndCreatedAtBeforeAndIdGreaterThanOrderByIdAsc(
            String provider,
            LocalDateTime cutoff,
            Long id,
            Pageable pageable
    );

    Optional<User> findByIdAndProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNull(Long id, String provider);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from User user
            where user.id = :userId
              and lower(user.provider) = lower(:provider)
              and user.emailVerifiedAt is null
              and user.deletedAt is null
            """)
    int deleteCleanupCandidate(@Param("userId") Long userId, @Param("provider") String provider);
}
