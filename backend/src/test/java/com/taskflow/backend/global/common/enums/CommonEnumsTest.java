package com.taskflow.backend.global.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonEnumsTest {

    @Test
    void 태스크_상태_ENUM은_명세값을_가진다() {
        assertThat(TaskStatus.values())
                .containsExactly(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
    }

    @Test
    void 태스크_우선순위_ENUM은_명세값을_가진다() {
        assertThat(TaskPriority.values())
                .containsExactly(TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.URGENT);
    }

    @Test
    void 프로젝트_권한_ENUM은_명세값을_가진다() {
        assertThat(ProjectRole.values())
                .containsExactly(ProjectRole.OWNER, ProjectRole.MEMBER);
    }

    @Test
    void 알림_타입_ENUM은_명세값을_가진다() {
        assertThat(NotificationType.values())
                .containsExactly(
                        NotificationType.TASK_ASSIGNED,
                        NotificationType.TASK_STATUS_CHANGED,
                        NotificationType.COMMENT_ADDED,
                        NotificationType.PROJECT_INVITED,
                        NotificationType.MEMBER_REMOVED
                );
    }

    @Test
    void 사용자_권한_및_상태_ENUM은_명세값을_가진다() {
        assertThat(Role.values()).containsExactly(Role.ROLE_USER, Role.ROLE_ADMIN);
        assertThat(UserStatus.values()).containsExactly(UserStatus.ACTIVE, UserStatus.LOCKED, UserStatus.DELETED);
    }
}
