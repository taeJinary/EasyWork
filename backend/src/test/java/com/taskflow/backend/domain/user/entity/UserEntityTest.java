package com.taskflow.backend.domain.user.entity;

import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void 기본값이_명세대로_초기화된다() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("tester")
                .build();

        assertThat(user.getProvider()).isEqualTo("LOCAL");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    void 프로필_수정은_null이_아닌_값만_반영한다() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("tester")
                .profileImg("old.png")
                .build();

        user.updateProfile("newName", null);

        assertThat(user.getNickname()).isEqualTo("newName");
        assertThat(user.getProfileImg()).isEqualTo("old.png");
    }

    @Test
    void 비밀번호_변경과_소프트삭제가_동작한다() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("tester")
                .build();

        user.changePassword("newEncoded");
        user.softDelete();

        assertThat(user.getPassword()).isEqualTo("newEncoded");
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isDeleted()).isTrue();
    }
}
