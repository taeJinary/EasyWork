package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.global.common.enums.OAuthProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OAuthClientRegistry {

    private final Map<OAuthProvider, OAuthClient> clientByProvider;

    public OAuthClientRegistry(List<OAuthClient> clients) {
        this.clientByProvider = clients.stream()
                .collect(Collectors.toUnmodifiableMap(
                        OAuthClient::provider,
                        Function.identity(),
                        (existing, replacement) -> {
                            throw new IllegalStateException("Duplicated OAuthClient for provider: " + existing.provider());
                        }
                ));
    }

    public OAuthClient getClient(OAuthProvider provider) {
        OAuthClient client = clientByProvider.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        }
        return client;
    }
}

