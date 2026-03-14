package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.domain.user.dto.response.OAuthAuthorizeUrlResponse;
import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OAuthAuthorizeUrlService {

    private static final String GOOGLE_SCOPE = "openid email profile";

    private final OAuthStateService oauthStateService;
    private final ProviderAuthorizeConfig googleConfig;
    private final ProviderAuthorizeConfig naverConfig;

    public OAuthAuthorizeUrlService(
            OAuthStateService oauthStateService,
            @Value("${app.oauth.google.client-id:}") String googleClientId,
            @Value("${app.oauth.google.redirect-uri:}") String googleRedirectUri,
            @Value("${app.oauth.naver.client-id:}") String naverClientId,
            @Value("${app.oauth.naver.redirect-uri:}") String naverRedirectUri
    ) {
        this.oauthStateService = oauthStateService;
        this.googleConfig = new ProviderAuthorizeConfig(
                "https://accounts.google.com/o/oauth2/v2/auth",
                googleClientId,
                googleRedirectUri
        );
        this.naverConfig = new ProviderAuthorizeConfig(
                "https://nid.naver.com/oauth2.0/authorize",
                naverClientId,
                naverRedirectUri
        );
    }

    public OAuthAuthorizeUrlResponse issue(OAuthProvider provider) {
        ProviderAuthorizeConfig config = resolveConfig(provider);
        validateConfig(config);

        String state = oauthStateService.issue(provider);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(config.authorizeUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.clientId())
                .queryParam("redirect_uri", config.redirectUri())
                .queryParam("state", state);

        if (provider == OAuthProvider.GOOGLE) {
            builder.queryParam("scope", GOOGLE_SCOPE);
        }

        return new OAuthAuthorizeUrlResponse(builder.build().encode().toUriString());
    }

    private ProviderAuthorizeConfig resolveConfig(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleConfig;
            case NAVER -> naverConfig;
        };
    }

    private void validateConfig(ProviderAuthorizeConfig config) {
        if (!StringUtils.hasText(config.authorizeUri())
                || !StringUtils.hasText(config.clientId())
                || !StringUtils.hasText(config.redirectUri())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private record ProviderAuthorizeConfig(
            String authorizeUri,
            String clientId,
            String redirectUri
    ) {
    }
}
