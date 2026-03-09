package com.taskflow.backend.domain.notification.repository;

import com.taskflow.backend.domain.notification.entity.NotificationPushToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPushTokenRepository extends JpaRepository<NotificationPushToken, Long> {

    Optional<NotificationPushToken> findByToken(String token);

    Optional<NotificationPushToken> findByUserIdAndToken(Long userId, String token);

    List<NotificationPushToken> findAllByUserIdAndIsActiveTrue(Long userId);

    List<NotificationPushToken> findAllByUserIdAndIsActiveTrueOrderByIdDesc(Long userId);
}
