package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.domain.user.dto.response.OAuthAuthorizeUrlResponse;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.infra.redis.RedisService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OAuthAuthorizeUrlServiceTest {

    @Test
    void issueStoresStateAndReturnsGoogleAuthorizeUrl() {
        RedisService redisService = mock(RedisService.class);
        OAuthStateService oauthStateService = new OAuthStateService(redisService);
        OAuthAuthorizeUrlService service = new OAuthAuthorizeUrlService(
                oauthStateService,
                "google-client-id",
                "http://localhost:5173/oauth/google/callback",
                "naver-client-id",
                "http://localhost:5173/oauth/naver/callback"
        );

        OAuthAuthorizeUrlResponse response = service.issue(OAuthProvider.GOOGLE);
        URLParts url = new URLParts(response.authorizeUrl());

        assertThat(url.originWithPath()).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(url.param("client_id")).isEqualTo("google-client-id");
        assertThat(url.param("redirect_uri")).isEqualTo("http://localhost:5173/oauth/google/callback");
        assertThat(url.param("scope")).isEqualTo("openid email profile");
        assertThat(url.param("state")).isNotBlank();
        verify(redisService).setValue(anyString(), anyString(), eq(Duration.ofMinutes(10)));
    }

    private record URLParts(java.net.URL url) {
        URLParts(String raw) {
            this(toUrl(raw));
        }

        String originWithPath() {
            return url.getProtocol() + "://" + url.getHost() + url.getPath();
        }

        String param(String key) {
            return java.util.Arrays.stream(url.getQuery().split("&"))
                    .map(part -> part.split("=", 2))
                    .filter(parts -> parts.length == 2 && parts[0].equals(key))
                    .map(parts -> java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8))
                    .findFirst()
                    .orElse(null);
        }

        private static java.net.URL toUrl(String raw) {
            try {
                return new java.net.URL(raw);
            } catch (java.net.MalformedURLException exception) {
                throw new IllegalArgumentException(exception);
            }
        }
    }
}
