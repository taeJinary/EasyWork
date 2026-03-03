package com.taskflow.backend.domain.user.service.oauth.client;

import com.taskflow.backend.domain.user.service.oauth.OAuthClient;
import com.taskflow.backend.domain.user.service.oauth.OAuthProfile;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverOAuthClient implements OAuthClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final String userInfoUri;

    public NaverOAuthClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.oauth.naver.user-info-uri:https://openapi.naver.com/v1/nid/me}") String userInfoUri
    ) {
        this.restClient = restClientBuilder.build();
        this.userInfoUri = userInfoUri;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.NAVER;
    }

    @Override
    public OAuthProfile fetchProfile(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        try {
            NaverUserInfoResponse response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(NaverUserInfoResponse.class);

            String providerId = response != null && response.response() != null
                    ? response.response().id()
                    : null;
            String email = response != null && response.response() != null
                    ? response.response().email()
                    : null;
            String nickname = resolveNickname(response);

            if (!StringUtils.hasText(providerId) || !StringUtils.hasText(email)) {
                throw new BusinessException(ErrorCode.OAUTH_PROFILE_INVALID);
            }

            return new OAuthProfile(providerId, email, nickname);
        } catch (RestClientResponseException ex) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String resolveNickname(NaverUserInfoResponse response) {
        if (response == null || response.response() == null) {
            return null;
        }

        String nickname = response.response().nickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        return response.response().name();
    }

    private record NaverUserInfoResponse(
            String resultcode,
            String message,
            NaverProfile response
    ) {
    }

    private record NaverProfile(
            String id,
            String email,
            String nickname,
            String name
    ) {
    }
}
