package com.taskflow.backend.global.auth.jwt;

import com.taskflow.backend.global.common.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void accessToken을_생성하고_검증_및_claim을_추출한다() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());

        String token = tokenProvider.generateAccessToken(1L, "user@example.com", Role.ROLE_USER);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserId(token)).isEqualTo(1L);
        assertThat(tokenProvider.getEmail(token)).isEqualTo("user@example.com");
        assertThat(tokenProvider.getRole(token)).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void 유효하지_않은_토큰은_검증에_실패한다() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());

        assertThat(tokenProvider.validateToken("invalid.token")).isFalse();
    }

    @Test
    void refreshToken을_생성하고_검증한다() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());

        String token = tokenProvider.generateRefreshToken(7L);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserId(token)).isEqualTo(7L);
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-signing-minimum-32chars");
        properties.setAccessTokenExpiration(1800000L);
        properties.setRefreshTokenExpiration(1209600000L);
        return properties;
    }
}
