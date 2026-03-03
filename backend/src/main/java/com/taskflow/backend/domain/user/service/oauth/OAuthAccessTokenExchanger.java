package com.taskflow.backend.domain.user.service.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OAuthAccessTokenExchanger {

    private final RestClient restClient;

    private final ProviderTokenConfig googleConfig;
    private final ProviderTokenConfig kakaoConfig;
    private final ProviderTokenConfig naverConfig;

    public OAuthAccessTokenExchanger(
            RestClient.Builder restClientBuilder,
            @Value("${app.oauth.google.token-uri:https://oauth2.googleapis.com/token}") String googleTokenUri,
            @Value("${app.oauth.google.client-id:}") String googleClientId,
            @Value("${app.oauth.google.client-secret:}") String googleClientSecret,
            @Value("${app.oauth.google.redirect-uri:}") String googleRedirectUri,
            @Value("${app.oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}") String kakaoTokenUri,
            @Value("${app.oauth.kakao.client-id:}") String kakaoClientId,
            @Value("${app.oauth.kakao.client-secret:}") String kakaoClientSecret,
            @Value("${app.oauth.kakao.redirect-uri:}") String kakaoRedirectUri,
            @Value("${app.oauth.naver.token-uri:https://nid.naver.com/oauth2.0/token}") String naverTokenUri,
            @Value("${app.oauth.naver.client-id:}") String naverClientId,
            @Value("${app.oauth.naver.client-secret:}") String naverClientSecret,
            @Value("${app.oauth.naver.redirect-uri:}") String naverRedirectUri
    ) {
        this.restClient = restClientBuilder.build();
        this.googleConfig = new ProviderTokenConfig(googleTokenUri, googleClientId, googleClientSecret, googleRedirectUri);
        this.kakaoConfig = new ProviderTokenConfig(kakaoTokenUri, kakaoClientId, kakaoClientSecret, kakaoRedirectUri);
        this.naverConfig = new ProviderTokenConfig(naverTokenUri, naverClientId, naverClientSecret, naverRedirectUri);
    }

    public String exchange(OAuthProvider provider, String authorizationCode, String codeVerifier) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        ProviderTokenConfig config = resolveConfig(provider);
        validateConfig(config);

        LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", authorizationCode);
        formData.add("client_id", config.clientId());
        formData.add("redirect_uri", config.redirectUri());
        if (StringUtils.hasText(config.clientSecret())) {
            formData.add("client_secret", config.clientSecret());
        }
        if (StringUtils.hasText(codeVerifier)) {
            formData.add("code_verifier", codeVerifier);
        }

        try {
            OAuthAccessTokenResponse response = restClient.post()
                    .uri(config.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(OAuthAccessTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private ProviderTokenConfig resolveConfig(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleConfig;
            case KAKAO -> kakaoConfig;
            case NAVER -> naverConfig;
        };
    }

    private void validateConfig(ProviderTokenConfig config) {
        if (!StringUtils.hasText(config.tokenUri())
                || !StringUtils.hasText(config.clientId())
                || !StringUtils.hasText(config.redirectUri())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private record ProviderTokenConfig(
            String tokenUri,
            String clientId,
            String clientSecret,
            String redirectUri
    ) {
    }

    private record OAuthAccessTokenResponse(
            @JsonProperty("access_token")
            String accessToken
    ) {
    }
}
