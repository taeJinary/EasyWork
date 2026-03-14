package com.taskflow.backend.global.auth.jwt;

import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.infra.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    public static final String AUTH_ERROR_CODE_ATTRIBUTE = "authErrorCode";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/auth/signup")
                || path.equals("/auth/login")
                || path.equals("/auth/oauth/authorize-url")
                || path.equals("/auth/oauth/code/login")
                || path.equals("/auth/token/reissue")
                || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtTokenProvider.TokenValidationResult validationResult = jwtTokenProvider.validateAccessToken(token);
            if (validationResult == JwtTokenProvider.TokenValidationResult.EXPIRED) {
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
                filterChain.doFilter(request, response);
                return;
            }
            if (validationResult == JwtTokenProvider.TokenValidationResult.INVALID) {
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, ErrorCode.TOKEN_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            String tokenId = jwtTokenProvider.getTokenId(token);
            if (tokenId != null && redisService.hasKey(BLACKLIST_KEY_PREFIX + tokenId)) {
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, ErrorCode.TOKEN_INVALID);
                filterChain.doFilter(request, response);
                return;
            }
            Long userId = jwtTokenProvider.getUserId(token);

            userRepository.findById(userId)
                    .filter(user -> !user.isDeleted())
                    .ifPresent(user -> {
                        CustomUserDetails userDetails = CustomUserDetails.from(user);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
