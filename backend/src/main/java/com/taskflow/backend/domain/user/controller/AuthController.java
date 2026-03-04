package com.taskflow.backend.domain.user.controller;

import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthCodeLoginRequest;
import com.taskflow.backend.domain.user.dto.request.OAuthLoginRequest;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long ONE_SECOND_IN_MILLIS = 1000L;

    @Value("${app.cookie.refresh-token-name:refresh_token}")
    private String refreshTokenCookieName;

    @Value("${app.cookie.refresh-token-path:/api/v1/auth}")
    private String refreshTokenCookiePath;

    @Value("${app.cookie.secure:false}")
    private boolean refreshTokenCookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String refreshTokenCookieSameSite;

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginTokens tokens = authService.login(request);
        addRefreshTokenCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresIn());

        LoginResponse loginResponse = new LoginResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                tokens.user()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthLogin(
            @Valid @RequestBody OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        LoginTokens tokens = authService.oauthLogin(request);
        addRefreshTokenCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresIn());

        LoginResponse loginResponse = new LoginResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                tokens.user()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/oauth/code/login")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthCodeLogin(
            @Valid @RequestBody OAuthCodeLoginRequest request,
            HttpServletResponse response
    ) {
        LoginTokens tokens = authService.oauthCodeLogin(request);
        addRefreshTokenCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresIn());

        LoginResponse loginResponse = new LoginResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                tokens.user()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/token/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
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

    @PostMapping("/logout")
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

