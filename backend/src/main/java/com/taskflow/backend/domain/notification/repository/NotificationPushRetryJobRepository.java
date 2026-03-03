package com.taskflow.backend.domain.notification.repository;

import com.taskflow.backend.domain.notification.entity.NotificationPushRetryJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPushRetryJobRepository extends JpaRepository<NotificationPushRetryJob, Long> {

    boolean existsByNotificationIdAndCompletedAtIsNull(Long notificationId);

    List<NotificationPushRetryJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime nextRetryAt,
            Pageable pageable
    );
}
