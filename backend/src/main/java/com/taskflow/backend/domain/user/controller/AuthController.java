package com.taskflow.backend.domain.user.controller;

import com.taskflow.backend.domain.user.dto.request.LoginRequest;
import com.taskflow.backend.domain.user.dto.request.LogoutRequest;
import com.taskflow.backend.domain.user.dto.request.ReissueRequest;
import com.taskflow.backend.domain.user.dto.request.SignupRequest;
import com.taskflow.backend.domain.user.dto.response.LoginResponse;
import com.taskflow.backend.domain.user.dto.response.ReissueResponse;
import com.taskflow.backend.domain.user.dto.response.SignupResponse;
import com.taskflow.backend.domain.user.service.AuthService;
import com.taskflow.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/token/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            @Valid @RequestBody ReissueRequest request
    ) {
        ReissueResponse response = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @Valid @RequestBody LogoutRequest request
    ) {
        String accessToken = resolveAccessToken(authorizationHeader);
        authService.logout(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    private String resolveAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

