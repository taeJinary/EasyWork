package com.taskflow.backend.infra.redis;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final DefaultRedisScript<Long> TRY_ACQUIRE_EMAIL_VERIFICATION_RESEND_SLOT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local cooldownKey = KEYS[1]
                    local countKey = KEYS[2]
                    local cooldownMillis = tonumber(ARGV[1])
                    local windowMillis = tonumber(ARGV[2])
                    local maxCount = tonumber(ARGV[3])

                    if redis.call('EXISTS', cooldownKey) == 1 then
                        return 0
                    end

                    local currentCount = tonumber(redis.call('GET', countKey) or '0')
                    if currentCount >= maxCount then
                        return 0
                    end

                    local updatedCount = redis.call('INCR', countKey)
                    if updatedCount == 1 then
                        redis.call('PEXPIRE', countKey, windowMillis)
                    end

                    redis.call('SET', cooldownKey, '1', 'PX', cooldownMillis)
                    return 1
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> ROLLBACK_EMAIL_VERIFICATION_RESEND_SLOT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local cooldownKey = KEYS[1]
                    local countKey = KEYS[2]

                    redis.call('DEL', cooldownKey)

                    local currentCount = tonumber(redis.call('GET', countKey) or '0')
                    if currentCount <= 1 then
                        redis.call('DEL', countKey)
                    else
                        redis.call('DECR', countKey)
                    end

                    return 1
                    """,
                    Long.class
            );

    private final StringRedisTemplate stringRedisTemplate;

    public void setValue(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    public Optional<String> getValue(String key) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    public void expire(String key, Duration ttl) {
        stringRedisTemplate.expire(key, ttl);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public boolean tryAcquireEmailVerificationResendSlot(
            String cooldownKey,
            String countKey,
            Duration cooldownTtl,
            Duration countWindowTtl,
            long maxCount
    ) {
        Long result = stringRedisTemplate.execute(
                TRY_ACQUIRE_EMAIL_VERIFICATION_RESEND_SLOT_SCRIPT,
                List.of(cooldownKey, countKey),
                String.valueOf(cooldownTtl.toMillis()),
                String.valueOf(countWindowTtl.toMillis()),
                String.valueOf(maxCount)
        );
        return Long.valueOf(1L).equals(result);
    }

    public void rollbackEmailVerificationResendSlot(String cooldownKey, String countKey) {
        stringRedisTemplate.execute(
                ROLLBACK_EMAIL_VERIFICATION_RESEND_SLOT_SCRIPT,
                List.of(cooldownKey, countKey)
        );
    }
}

