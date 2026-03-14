package com.taskflow.backend.domain.user.controller;

import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.EmailVerificationResendRequest;
import com.taskflow.backend.domain.user.dto.request.EmailVerificationVerifyRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthCodeLoginRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.LoginResponse;
import com.taskflow.backend.domain.user.dto.response.ReissueResponse;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.service.AuthService;
import com.taskflow.backend.domain.user.service.model.LoginTokens;
import com.taskflow.backend.domain.user.service.model.ReissueTokens;
import com.taskflow.backend.global.common.dto.ApiResponse;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.security.ApiRateLimitService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuthHttpContract.AUTH_BASE_PATH)
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long ONE_SECOND_IN_MILLIS = 1000L;

    @Value("${app.cookie.refresh-token-name:" + AuthHttpContract.REFRESH_TOKEN_COOKIE_NAME + "}")
    private String refreshTokenCookieName;

    @Value("${app.cookie.refresh-token-path:" + AuthHttpContract.REFRESH_TOKEN_COOKIE_PATH + "}")
    private String refreshTokenCookiePath;

    @Value("${app.cookie.secure:false}")
    private boolean refreshTokenCookieSecure;

    @Value("${app.cookie.same-site:" + AuthHttpContract.REFRESH_TOKEN_COOKIE_SAME_SITE + "}")
    private String refreshTokenCookieSameSite;

    private final AuthService authService;
    private final ApiRateLimitService apiRateLimitService;

    @PostMapping(AuthHttpContract.SIGNUP_PATH)
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }

    @PostMapping(AuthHttpContract.EMAIL_VERIFICATION_BASE_PATH + AuthHttpContract.EMAIL_VERIFICATION_VERIFY_PATH)
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody EmailVerificationVerifyRequest request
    ) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok(ApiResponse.success(null, "이메일 인증이 완료되었습니다."));
    }

    @PostMapping(AuthHttpContract.EMAIL_VERIFICATION_BASE_PATH + AuthHttpContract.EMAIL_VERIFICATION_RESEND_PATH)
    public ResponseEntity<ApiResponse<Void>> resendEmailVerification(
            @Valid @RequestBody EmailVerificationResendRequest request
    ) {
        authService.resendEmailVerification(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, "인증 메일을 다시 보냈습니다."));
    }

    @PostMapping(AuthHttpContract.LOGIN_PATH)
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response
    ) {
        apiRateLimitService.checkAuthLogin(httpServletRequest, request.email());
        LoginTokens tokens = authService.login(request);
        addRefreshTokenCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresIn());

        LoginResponse loginResponse = new LoginResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                tokens.user()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping(AuthHttpContract.OAUTH_CODE_LOGIN_PATH)
    public ResponseEntity<ApiResponse<LoginResponse>> oauthCodeLogin(
            @Valid @RequestBody OAuthCodeLoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response
    ) {
        apiRateLimitService.checkAuthOauthCodeLogin(httpServletRequest);
        LoginTokens tokens = authService.oauthCodeLogin(request);
        addRefreshTokenCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresIn());

        LoginResponse loginResponse = new LoginResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                tokens.user()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping(AuthHttpContract.TOKEN_REISSUE_PATH)
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        apiRateLimitService.checkAuthTokenReissue(request);
        String refreshToken = resolveRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        ReissueTokens tokens = authService.reissue(refreshToken);
        addRefreshTokenCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresIn());

        ReissueResponse reissueResponse = new ReissueResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresIn()
        );
        return ResponseEntity.ok(ApiResponse.success(reissueResponse));
    }

    @PostMapping(AuthHttpContract.LOGOUT_PATH)
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String accessToken = resolveAccessToken(authorizationHeader);
        authService.logout(accessToken, refreshToken);
        expireRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    private String resolveAccessToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return accessToken;
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long expiresInMillis) {
        long maxAgeSeconds = Math.max(expiresInMillis / ONE_SECOND_IN_MILLIS, 0);
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(refreshTokenCookiePath)
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void expireRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(refreshTokenCookiePath)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

