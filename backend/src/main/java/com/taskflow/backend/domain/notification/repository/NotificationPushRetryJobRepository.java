package com.taskflow.backend.domain.notification.repository;

import com.taskflow.backend.domain.notification.entity.NotificationPushRetryJob;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationPushRetryJobRepository extends JpaRepository<NotificationPushRetryJob, Long> {

    boolean existsByNotificationIdAndPushTokenIdAndCompletedAtIsNull(Long notificationId, Long pushTokenId);

    long countByCompletedAtIsNull();

    List<NotificationPushRetryJob> findByCompletedAtIsNullAndNextRetryAtLessThanEqualOrderByIdAsc(
            LocalDateTime nextRetryAt,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from NotificationPushRetryJob job
            where job.completedAt is not null
              and job.updatedAt < :cutoff
            """)
    int deleteCompletedHistoryBefore(@Param("cutoff") LocalDateTime cutoff);
}
