package com.taskflow.backend.domain.user.service;

import com.taskflow.backend.domain.user.entity.EmailVerificationToken;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.EmailVerificationTokenRepository;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenManagerTest {

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailVerificationTokenGenerator emailVerificationTokenGenerator;

    @Mock
    private EmailVerificationMailService emailVerificationMailService;

    @InjectMocks
    private EmailVerificationTokenManager emailVerificationTokenManager;

    @Test
    void issueStoresTokenHashAndSendsVerificationMail() {
        User user = localUser();

        when(emailVerificationMailService.isReady()).thenReturn(true);
        when(emailVerificationTokenGenerator.generate()).thenReturn("raw-token");

        emailVerificationTokenManager.issue(user);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
        verify(emailVerificationMailService).sendVerificationEmail(user.getEmail(), "raw-token");
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(EmailVerificationTokenHashUtils.hash("raw-token"));
    }

    @Test
    void reissueRevokesExistingActiveTokensBeforeIssuingNewToken() {
        User user = localUser();
        EmailVerificationToken existingToken = EmailVerificationToken.create(
                user,
                "old-hash",
                LocalDateTime.now().plusHours(1)
        );

        when(emailVerificationMailService.isReady()).thenReturn(true);
        when(emailVerificationTokenRepository.findAllByUserIdAndConsumedAtIsNullAndRevokedAtIsNull(user.getId()))
                .thenReturn(List.of(existingToken));
        when(emailVerificationTokenGenerator.generate()).thenReturn("next-token");

        emailVerificationTokenManager.reissue(user);

        assertThat(existingToken.getRevokedAt()).isNotNull();
        verify(emailVerificationMailService).sendVerificationEmail(user.getEmail(), "next-token");
    }

    @Test
    void issueThrowsWhenMailServiceIsNotReady() {
        when(emailVerificationMailService.isReady()).thenReturn(false);

        assertThatThrownBy(() -> emailVerificationTokenManager.issue(localUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ready");

        verify(emailVerificationTokenRepository, org.mockito.Mockito.never()).save(any(EmailVerificationToken.class));
    }

    private User localUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .nickname("user")
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
