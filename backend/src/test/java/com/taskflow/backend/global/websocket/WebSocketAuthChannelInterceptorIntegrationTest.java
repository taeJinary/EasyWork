package com.taskflow.backend.global.websocket;

import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.auth.jwt.JwtTokenProvider;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.support.IntegrationTestContainerSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WebSocketAuthChannelInterceptorIntegrationTest extends IntegrationTestContainerSupport {

    @Autowired
    private WebSocketAuthChannelInterceptor interceptor;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    void subscribeProjectBoardAllowsProjectMember() {
        User member = saveUser("member");
        Workspace workspace = workspaceRepository.save(Workspace.create(member, "ws-member", null));
        Project project = projectRepository.save(Project.builder()
                .owner(member)
                .workspace(workspace)
                .name("project")
                .description("desc")
                .build());
        projectMemberRepository.save(ProjectMember.create(project, member, ProjectRole.OWNER, LocalDateTime.now()));

        Authentication authentication = connectAs(member);
        Message<byte[]> subscribeMessage =
                subscribeMessage(WebSocketContract.projectBoardDestination(project.getId()), authentication);

        assertThatCode(() -> interceptor.preSend(subscribeMessage, null)).doesNotThrowAnyException();
    }

    @Test
    void subscribeProjectBoardRejectsNonMember() {
        User owner = saveUser("owner");
        User outsider = saveUser("outsider");
        Workspace workspace = workspaceRepository.save(Workspace.create(owner, "ws-owner", null));
        Project project = projectRepository.save(Project.builder()
                .owner(owner)
                .workspace(workspace)
                .name("project")
                .description("desc")
                .build());
        projectMemberRepository.save(ProjectMember.create(project, owner, ProjectRole.OWNER, LocalDateTime.now()));

        Authentication authentication = connectAs(outsider);
        Message<byte[]> subscribeMessage =
                subscribeMessage(WebSocketContract.projectBoardDestination(project.getId()), authentication);

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private Authentication connectAs(User user) {
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setNativeHeader("Authorization", "Bearer " + token);
        connectAccessor.setLeaveMutable(true);
        Message<byte[]> connectMessage = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());
        Message<?> authenticated = interceptor.preSend(connectMessage, null);

        StompHeaderAccessor authenticatedAccessor = StompHeaderAccessor.wrap(authenticated);
        assertThat(authenticatedAccessor.getUser()).isInstanceOf(Authentication.class);
        return (Authentication) authenticatedAccessor.getUser();
    }

    private Message<byte[]> subscribeMessage(String destination, Authentication authentication) {
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setDestination(destination);
        subscribeAccessor.setUser(authentication);
        return MessageBuilder.createMessage(new byte[0], subscribeAccessor.getMessageHeaders());
    }

    private User saveUser(String prefix) {
        return userRepository.save(User.builder()
                .email(prefix + System.nanoTime() + "@example.com")
                .password("encoded-password")
                .nickname(prefix + "-user")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
