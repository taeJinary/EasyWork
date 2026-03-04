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
public class GoogleOAuthClient implements OAuthClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final String userInfoUri;

    public GoogleOAuthClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.oauth.google.user-info-uri:https://www.googleapis.com/oauth2/v3/userinfo}") String userInfoUri
    ) {
        this.restClient = restClientBuilder.build();
        this.userInfoUri = userInfoUri;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthProfile fetchProfile(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        }

        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null
                    || !StringUtils.hasText(response.sub())
                    || !StringUtils.hasText(response.email())) {
                throw new BusinessException(ErrorCode.OAUTH_PROFILE_INVALID);
            }

            return new OAuthProfile(response.sub(), response.email(), response.name());
        } catch (RestClientResponseException ex) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_INVALID);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private record GoogleUserInfoResponse(
            String sub,
            String email,
            String name
    ) {
    }
}

