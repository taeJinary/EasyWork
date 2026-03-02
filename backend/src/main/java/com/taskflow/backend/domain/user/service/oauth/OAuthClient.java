package com.taskflow.backend.domain.user.service.oauth;

import com.taskflow.backend.global.common.enums.OAuthProvider;

public interface OAuthClient {

    OAuthProvider provider();

    OAuthProfile fetchProfile(String accessToken);
}

