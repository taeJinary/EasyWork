package com.taskflow.backend.global.config;

import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigCorsValidationTest {

    @Test
    void corsConfigurationRejectsWildcardOriginWhenCredentialsEnabled() {
        SecurityConfig securityConfig = new SecurityConfig(
                Mockito.mock(JwtAuthenticationFilter.class),
                Mockito.mock(ApiAuthenticationEntryPoint.class),
                Mockito.mock(ApiAccessDeniedHandler.class),
                List.of("*")
        );

        assertThatThrownBy(securityConfig::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allowed-origins");
    }

    @Test
    void corsConfigurationAllowsExplicitOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(
                Mockito.mock(JwtAuthenticationFilter.class),
                Mockito.mock(ApiAuthenticationEntryPoint.class),
                Mockito.mock(ApiAccessDeniedHandler.class),
                List.of("http://localhost:5173", "http://127.0.0.1:5173")
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations()
                .get("/**");

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
