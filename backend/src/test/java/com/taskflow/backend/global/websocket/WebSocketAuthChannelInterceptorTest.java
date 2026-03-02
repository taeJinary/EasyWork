package com.taskflow.backend.global.websocket;

import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.auth.CustomUserDetails;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.infra.redis.RedisService;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenProvider,
                userRepository,
                redisService,
                projectMemberRepository
        );
    }

    @Test
    void connectWithValidAuthorizationSetsPrincipal() {
        User user = activeUser(1L, "user@example.com");
        Message<?> message = connectMessage("Bearer valid-token");

        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(JwtTokenProvider.TokenValidationResult.VALID);
        given(jwtTokenProvider.getTokenId("valid-token")).willReturn("jti-123");
        given(redisService.hasKey("blacklist:jti-123")).willReturn(false);
        given(jwtTokenProvider.getUserId("valid-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        Message<?> result = interceptor.preSend(message, messageChannel);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        Principal principal = accessor.getUser();

        assertThat(principal).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(principal.getName()).isEqualTo("user@example.com");
    }

    @Test
    void connectWithoutAuthorizationThrowsUnauthorized() {
        Message<?> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void connectWithBlacklistedTokenThrowsTokenInvalid() {
        Message<?> message = connectMessage("Bearer valid-token");

        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(JwtTokenProvider.TokenValidationResult.VALID);
        given(jwtTokenProvider.getTokenId("valid-token")).willReturn("jti-123");
        given(redisService.hasKey("blacklist:jti-123")).willReturn(true);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    void connectWithExpiredTokenThrowsTokenExpired() {
        Message<?> message = connectMessage("Bearer expired-token");

        given(jwtTokenProvider.validateAccessToken("expired-token"))
                .willReturn(JwtTokenProvider.TokenValidationResult.EXPIRED);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void subscribeProjectTopicThrowsForbiddenWhenNotProjectMember() {
        Message<?> message = subscribeMessage("/topic/projects/10/board", authPrincipal(1L, "user@example.com"));

        given(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void subscribePersonalNotificationQueueIsAllowedForAuthenticatedUser() {
        Authentication principal = authPrincipal(1L, "user@example.com");
        Message<?> message = subscribeMessage("/user/queue/notifications", principal);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isSameAs(message);
    }

    @Test
    void subscribePersonalQueueWithUserScopedPathThrowsForbidden() {
        Message<?> message = subscribeMessage("/user/2/queue/notifications", authPrincipal(1L, "user@example.com"));

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void subscribeUnknownDestinationThrowsForbidden() {
        Message<?> message = subscribeMessage("/topic/notifications/global", authPrincipal(1L, "user@example.com"));

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void subscribePersonalNotificationQueueWithInvalidPrincipalThrowsUnauthorized() {
        UsernamePasswordAuthenticationToken invalidPrincipal =
                new UsernamePasswordAuthenticationToken("user@example.com", null);
        Message<?> message = subscribeMessage("/user/queue/notifications", invalidPrincipal);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void subscribeProjectTopicAllowsProjectMember() {
        Message<?> message = subscribeMessage("/topic/projects/10/board", authPrincipal(1L, "user@example.com"));

        given(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).willReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isSameAs(message);
    }

    private Message<?> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> subscribeMessage(String destination, Authentication principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UsernamePasswordAuthenticationToken authPrincipal(Long userId, String email) {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                email,
                "encoded",
                "ROLE_USER",
                UserStatus.ACTIVE
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User activeUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .nickname("tester")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
