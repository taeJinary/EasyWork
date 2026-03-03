package com.taskflow.backend.domain.user.service.oauth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class KakaoOAuthClient implements OAuthClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final String userInfoUri;

    public KakaoOAuthClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}") String userInfoUri
    ) {
        this.restClient = restClientBuilder.build();
        this.userInfoUri = userInfoUri;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthProfile fetchProfile(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            String providerId = extractProviderId(response);
            String email = response != null && response.kakaoAccount() != null
                    ? response.kakaoAccount().email()
                    : null;
            String nickname = response != null
                    && response.kakaoAccount() != null
                    && response.kakaoAccount().profile() != null
                    ? response.kakaoAccount().profile().nickname()
                    : null;

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

    private String extractProviderId(KakaoUserInfoResponse response) {
        if (response == null || response.id() == null) {
            return null;
        }
        return String.valueOf(response.id());
    }

    private record KakaoUserInfoResponse(
            Long id,
            @JsonProperty("kakao_account")
            KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    private record KakaoProfile(
            String nickname
    ) {
    }
}
