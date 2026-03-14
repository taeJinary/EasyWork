package com.taskflow.backend.global.auth.jwt;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.infra.redis.RedisService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private RedisService redisService;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-signing-minimum-32chars");
        properties.setAccessTokenExpiration(1800000L);
        properties.setRefreshTokenExpiration(1209600000L);

        jwtTokenProvider = new JwtTokenProvider(properties);
        userRepository = Mockito.mock(UserRepository.class);
        redisService = Mockito.mock(RedisService.class);
        when(redisService.hasKey(anyString())).thenReturn(false);

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository, redisService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authExcludedPathPassesWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.setServletPath("/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void oauthLoginPathIsNotExcludedWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/oauth/login");
        request.setServletPath("/auth/oauth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void oauthCodeLoginExcludedPathPassesWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/oauth/code/login");
        request.setServletPath("/auth/oauth/code/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validBearerTokenSetsAuthentication() throws Exception {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .nickname("tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String token = jwtTokenProvider.generateAccessToken(1L, user.getEmail(), user.getRole());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/projects/1");
        request.setServletPath("/projects/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user@example.com");
    }

    @Test
    void invalidTokenDoesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/projects/1");
        request.setServletPath("/projects/1");
        request.addHeader("Authorization", "Bearer invalid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void blacklistedTokenDoesNotSetAuthentication() throws Exception {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .nickname("tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenProvider.generateAccessToken(1L, user.getEmail(), user.getRole());
        when(redisService.hasKey("blacklist:" + jwtTokenProvider.getTokenId(token))).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/projects/1");
        request.setServletPath("/projects/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
