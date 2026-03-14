package com.taskflow.backend.domain.user.controller;

public final class AuthHttpContract {

    public static final String AUTH_BASE_PATH = "/auth";
    public static final String SIGNUP_PATH = "/signup";
    public static final String LOGIN_PATH = "/login";
    public static final String EMAIL_VERIFICATION_BASE_PATH = "/email-verification";
    public static final String EMAIL_VERIFICATION_VERIFY_PATH = "/verify";
    public static final String EMAIL_VERIFICATION_RESEND_PATH = "/resend";
    public static final String OAUTH_AUTHORIZE_URL_PATH = "/oauth/authorize-url";
    public static final String OAUTH_CODE_LOGIN_PATH = "/oauth/code/login";
    public static final String TOKEN_REISSUE_PATH = "/token/reissue";
    public static final String LOGOUT_PATH = "/logout";

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";
    public static final String REFRESH_TOKEN_COOKIE_SAME_SITE = "Lax";
    public static final String OAUTH_STATE_COOKIE_NAME_PREFIX = "oauth_state_nonce_";
    public static final String OAUTH_STATE_COOKIE_PATH = "/api/v1/auth/oauth/code/login";
    public static final String OAUTH_STATE_COOKIE_SAME_SITE = "Lax";

    private AuthHttpContract() {
    }
}
