package com.taskflow.backend.global.websocket;

import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.ops.OperationalMetricsService;
import com.taskflow.backend.infra.redis.RedisService;
import java.security.Principal;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String USER_DESTINATION_PREFIX = WebSocketContract.USER_PREFIX + "/";
    private static final Set<String> ALLOWED_USER_DESTINATIONS = Set.of(
            WebSocketContract.NOTIFICATION_QUEUE_DESTINATION
    );
    private static final Pattern PROJECT_BOARD_DESTINATION = WebSocketContract.projectBoardDestinationPattern();

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final ProjectMemberRepository projectMemberRepository;
    private final OperationalMetricsService operationalMetricsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            try {
                authenticate(accessor);
            } catch (BusinessException exception) {
                operationalMetricsService.incrementWebSocketConnectFailure();
                throw exception;
            }
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        String token = resolveBearerToken(authorizationHeader);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        JwtTokenProvider.TokenValidationResult validationResult = jwtTokenProvider.validateAccessToken(token);
        if (validationResult == JwtTokenProvider.TokenValidationResult.EXPIRED) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        if (validationResult == JwtTokenProvider.TokenValidationResult.INVALID) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        String tokenId = jwtTokenProvider.getTokenId(token);
        if (tokenId != null && redisService.hasKey(BLACKLIST_KEY_PREFIX + tokenId)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(token);
        User user = userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        CustomUserDetails userDetails = CustomUserDetails.from(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        accessor.setUser(authentication);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith(USER_DESTINATION_PREFIX)) {
            if (!ALLOWED_USER_DESTINATIONS.contains(destination)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            extractUserId(principal);
            return;
        }

        Matcher matcher = PROJECT_BOARD_DESTINATION.matcher(destination);
        if (matcher.matches()) {
            Long userId = extractUserId(principal);
            Long projectId = Long.valueOf(matcher.group(1));
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
            if (!isMember) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
