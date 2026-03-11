package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnverifiedUserCleanupService {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final UnverifiedUserCleanupProcessor unverifiedUserCleanupProcessor;

    @Value("${app.email-verification.cleanup.retention-hours:24}")
    private long retentionHours;

    @Transactional
    public int cleanupExpiredUnverifiedUsers(int batchSize) {
        int normalizedBatchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(Math.max(retentionHours, 1L));
        int deletedCount = 0;
        long lastProcessedId = 0L;

        while (true) {
            List<User> candidates =
                    userRepository.findByProviderIgnoreCaseAndEmailVerifiedAtIsNullAndDeletedAtIsNullAndCreatedAtBeforeAndIdGreaterThanOrderByIdAsc(
                            UnverifiedUserCleanupProcessor.LOCAL_PROVIDER,
                            cutoff,
                            lastProcessedId,
                            PageRequest.of(0, normalizedBatchSize)
                    );

            if (candidates.isEmpty()) {
                break;
            }

            for (User candidate : candidates) {
                lastProcessedId = candidate.getId();
                try {
                    if (unverifiedUserCleanupProcessor.processCandidate(candidate.getId())
                            == UnverifiedUserCleanupProcessor.CleanupResult.DELETED) {
                        deletedCount++;
                    }
                } catch (UnverifiedUserCleanupProcessor.ConcurrentCleanupSkipException exception) {
                    log.info("Skipped cleanup after candidate changed before delete. userId={}", candidate.getId());
                }
            }

            if (candidates.size() < normalizedBatchSize) {
                break;
            }
        }
        return deletedCount;
    }
}
