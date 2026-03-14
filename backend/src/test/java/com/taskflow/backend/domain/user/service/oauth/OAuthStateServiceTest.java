package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthStateServiceTest {

    @Test
    void issueStoresStateUsingClientNonceBoundKey() {
        RedisService redisService = mock(RedisService.class);
        OAuthStateService service = new OAuthStateService(redisService);

        String state = service.issue(OAuthProvider.GOOGLE, "client-nonce");

        verify(redisService).setValue(
                eq("oauth:state:google:client-nonce:" + state),
                eq(state),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void consumeExpectedStateThrowsWhenClientNonceDoesNotMatchIssuedState() {
        RedisService redisService = mock(RedisService.class);
        OAuthStateService service = new OAuthStateService(redisService);
        when(redisService.getValue(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumeExpectedState(OAuthProvider.GOOGLE, "issued-state", "other-nonce"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_TOKEN_INVALID);
    }
}
