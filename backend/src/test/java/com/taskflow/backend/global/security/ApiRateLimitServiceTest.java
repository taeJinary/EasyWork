package com.taskflow.backend.global.security;

import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiRateLimitServiceTest {

    private RedisService redisService;
    private ApiRateLimitService apiRateLimitService;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        apiRateLimitService = new ApiRateLimitService(
                redisService,
                3, 2, 60,
                5, 60,
                5, 60,
                5, 60,
                5, 60,
                5, 60,
                5, 60,
                5, 60,
                5, 60
        );
    }

    @Test
    void checkAuthLoginIncrementsIpAndEmailAndSetsTtlOnFirstHit() {
        when(redisService.increment(anyString())).thenReturn(1L);

        apiRateLimitService.checkAuthLogin(requestWithIp("203.0.113.10"), "USER@example.com");

        verify(redisService).increment("rate-limit:auth:login:ip:203.0.113.10");
        verify(redisService).increment("rate-limit:auth:login:email:user@example.com");
        verify(redisService).expire("rate-limit:auth:login:ip:203.0.113.10", Duration.ofSeconds(60));
        verify(redisService).expire("rate-limit:auth:login:email:user@example.com", Duration.ofSeconds(60));
    }

    @Test
    void checkAuthLoginThrowsTooManyRequestsWhenEmailLimitExceeded() {
        when(redisService.increment("rate-limit:auth:login:ip:203.0.113.10")).thenReturn(1L);
        when(redisService.increment("rate-limit:auth:login:email:user@example.com")).thenReturn(3L);

        assertThatThrownBy(() -> apiRateLimitService.checkAuthLogin(requestWithIp("203.0.113.10"), "user@example.com"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void checkInvitationCreateUsesUnknownKeyWhenUserIdIsNull() {
        when(redisService.increment("rate-limit:invitation:create:ip:198.51.100.10")).thenReturn(1L);
        when(redisService.increment("rate-limit:invitation:create:user:unknown")).thenReturn(6L);

        assertThatThrownBy(() -> apiRateLimitService.checkInvitationCreate(requestWithIp("198.51.100.10"), null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void checkInvitationCreateIncrementsIpAndUser() {
        when(redisService.increment("rate-limit:invitation:create:ip:198.51.100.10")).thenReturn(1L);
        when(redisService.increment("rate-limit:invitation:create:user:55")).thenReturn(1L);

        apiRateLimitService.checkInvitationCreate(requestWithIp("198.51.100.10"), 55L);

        verify(redisService).expire("rate-limit:invitation:create:ip:198.51.100.10", Duration.ofSeconds(60));
        verify(redisService).expire("rate-limit:invitation:create:user:55", Duration.ofSeconds(60));
    }

    @Test
    void checkAuthOauthLoginUsesRemoteAddrEvenWhenForwardedHeaderPresent() {
        when(redisService.increment(anyString())).thenReturn(1L);
        MockHttpServletRequest request = requestWithIp("10.0.0.10");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.10");

        apiRateLimitService.checkAuthOauthLogin(request);

        verify(redisService).increment("rate-limit:auth:oauth-login:ip:10.0.0.10");
    }

    @Test
    void checkCommentCreateFailsOpenWhenRedisIncrementReturnsNull() {
        when(redisService.increment("rate-limit:comment:create:user:1")).thenReturn(null);

        assertThatCode(() -> apiRateLimitService.checkCommentCreate(1L))
                .doesNotThrowAnyException();
        verify(redisService, never()).expire("rate-limit:comment:create:user:1", Duration.ofSeconds(60));
    }

    @Test
    void checkPushTokenRegisterUsesUserKey() {
        when(redisService.increment("rate-limit:notification:push-token-register:user:55")).thenReturn(1L);

        apiRateLimitService.checkPushTokenRegister(55L);

        verify(redisService).expire(
                "rate-limit:notification:push-token-register:user:55",
                Duration.ofSeconds(60)
        );
    }

    @Test
    void checkAuthLoginNormalizesEmailWithLocaleRoot() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            when(redisService.increment("rate-limit:auth:login:ip:203.0.113.10")).thenReturn(1L);
            when(redisService.increment("rate-limit:auth:login:email:iuser@example.com")).thenReturn(1L);

            apiRateLimitService.checkAuthLogin(requestWithIp("203.0.113.10"), "IUSER@example.com");

            verify(redisService).increment("rate-limit:auth:login:email:iuser@example.com");
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    private MockHttpServletRequest requestWithIp(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
