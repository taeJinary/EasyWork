package com.taskflow.backend.domain.user.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthHttpContractTest {

    @Test
    void authEndpointPathsRemainStable() {
        assertThat(AuthHttpContract.AUTH_BASE_PATH).isEqualTo("/auth");
        assertThat(AuthHttpContract.SIGNUP_PATH).isEqualTo("/signup");
        assertThat(AuthHttpContract.LOGIN_PATH).isEqualTo("/login");
        assertThat(AuthHttpContract.OAUTH_AUTHORIZE_URL_PATH).isEqualTo("/oauth/authorize-url");
        assertThat(AuthHttpContract.OAUTH_CODE_LOGIN_PATH).isEqualTo("/oauth/code/login");
        assertThat(AuthHttpContract.TOKEN_REISSUE_PATH).isEqualTo("/token/reissue");
        assertThat(AuthHttpContract.LOGOUT_PATH).isEqualTo("/logout");
        assertThat(AuthHttpContract.EMAIL_VERIFICATION_BASE_PATH).isEqualTo("/email-verification");
        assertThat(AuthHttpContract.EMAIL_VERIFICATION_VERIFY_PATH).isEqualTo("/verify");
        assertThat(AuthHttpContract.EMAIL_VERIFICATION_RESEND_PATH).isEqualTo("/resend");
    }

    @Test
    void refreshCookieContractRemainsStable() {
        assertThat(AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME).isEqualTo("refresh_token");
        assertThat(AuthHttpContract.REFRESH_TOKEN_COOKIE_PATH).isEqualTo("/api/v1/auth");
        assertThat(AuthHttpContract.REFRESH_TOKEN_COOKIE_SAME_SITE).isEqualTo("Lax");
        assertThat(AuthHttpContract.OAUTH_STATE_COOKIE_NAME_PREFIX).isEqualTo("oauth_state_nonce_");
        assertThat(AuthHttpContract.OAUTH_STATE_COOKIE_PATH).isEqualTo("/api/v1/auth/oauth/code/login");
        assertThat(AuthHttpContract.OAUTH_STATE_COOKIE_SAME_SITE).isEqualTo("Lax");
    }
}
