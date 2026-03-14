package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OAuthStateService {

    private static final String KEY_PREFIX = "oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final RedisService redisService;

    public OAuthStateService(RedisService redisService) {
        this.redisService = redisService;
    }

    public String issue(OAuthProvider provider) {
        String state = UUID.randomUUID().toString();
        redisService.setValue(stateKey(provider, state), state, STATE_TTL);
        return state;
    }

    public void consumeExpectedState(OAuthProvider provider, String state) {
        if (!StringUtils.hasText(state)) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        String key = stateKey(provider, state);
        if (redisService.getValue(key).isEmpty()) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        redisService.delete(key);
    }

    private String stateKey(OAuthProvider provider, String state) {
        return KEY_PREFIX + provider.name().toLowerCase(Locale.ROOT) + ":" + state.trim();
    }
}
