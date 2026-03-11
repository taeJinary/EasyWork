package com.taskflow.backend.infra.redis;

import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisServiceIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private RedisService redisService;

    @Test
    void tryAcquireEmailVerificationResendSlotAllowsOnlyOneConcurrentReservation() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String cooldownKey = "test:email-verification:cooldown:" + suffix;
        String countKey = "test:email-verification:count:" + suffix;
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return redisService.tryAcquireEmailVerificationResendSlot(
                            cooldownKey,
                            countKey,
                            Duration.ofSeconds(60),
                            Duration.ofHours(1),
                            5L
                    );
                }));
            }

            ready.await();
            start.countDown();

            long successCount = 0L;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                }
            }

            assertThat(successCount).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
            redisService.delete(cooldownKey);
            redisService.delete(countKey);
        }
    }

    @Test
    void rollbackEmailVerificationResendSlotRestoresAvailability() {
        String suffix = UUID.randomUUID().toString();
        String cooldownKey = "test:email-verification:cooldown:" + suffix;
        String countKey = "test:email-verification:count:" + suffix;

        try {
            boolean reserved = redisService.tryAcquireEmailVerificationResendSlot(
                    cooldownKey,
                    countKey,
                    Duration.ofSeconds(60),
                    Duration.ofHours(1),
                    5L
            );

            assertThat(reserved).isTrue();
            assertThat(redisService.hasKey(cooldownKey)).isTrue();
            assertThat(redisService.getValue(countKey)).hasValue("1");

            redisService.rollbackEmailVerificationResendSlot(cooldownKey, countKey);

            assertThat(redisService.hasKey(cooldownKey)).isFalse();
            assertThat(redisService.getValue(countKey)).isEmpty();
            assertThat(redisService.tryAcquireEmailVerificationResendSlot(
                    cooldownKey,
                    countKey,
                    Duration.ofSeconds(60),
                    Duration.ofHours(1),
                    5L
            )).isTrue();
        } finally {
            redisService.delete(cooldownKey);
            redisService.delete(countKey);
        }
    }
}
