package com.taskflow.backend.global.auth;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void User기반_UserDetails를_생성한다() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded")
                .nickname("tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        CustomUserDetails details = CustomUserDetails.from(user);

        assertThat(details.getUserId()).isEqualTo(1L);
        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void 잠긴_사용자는_AccountNonLocked가_false다() {
        User lockedUser = User.builder()
                .id(1L)
                .email("lock@example.com")
                .password("encoded")
                .nickname("tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.LOCKED)
                .build();

        CustomUserDetails details = CustomUserDetails.from(lockedUser);

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void 삭제된_사용자는_enabled가_false다() {
        User deletedUser = User.builder()
                .id(1L)
                .email("deleted@example.com")
                .password("encoded")
                .nickname("tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.DELETED)
                .build();

        CustomUserDetails details = CustomUserDetails.from(deletedUser);

        assertThat(details.isEnabled()).isFalse();
    }
}
