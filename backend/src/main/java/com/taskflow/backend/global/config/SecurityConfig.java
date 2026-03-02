package com.taskflow.backend.global.config;

import com.taskflow.backend.global.auth.jwt.JwtAuthenticationFilter;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") List<String> allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
        this.apiAccessDeniedHandler = apiAccessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler)
                )
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/signup", "/auth/login", "/auth/oauth/login", "/auth/token/reissue").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        validateAllowedOrigins();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void validateAllowedOrigins() {
        boolean hasWildcard = allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch("*"::equals);
        if (hasWildcard) {
            throw new IllegalStateException("app.cors.allowed-origins cannot contain '*' when credentials are enabled");
        }
    }
}
