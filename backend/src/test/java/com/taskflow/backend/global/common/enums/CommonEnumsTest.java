package com.taskflow.backend.global.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonEnumsTest {

    @Test
    void taskStatusEnumMatchesSpecification() {
        assertThat(TaskStatus.values())
                .containsExactly(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
    }

    @Test
    void taskPriorityEnumMatchesSpecification() {
        assertThat(TaskPriority.values())
                .containsExactly(TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.URGENT);
    }

    @Test
    void projectRoleEnumMatchesSpecification() {
        assertThat(ProjectRole.values())
                .containsExactly(ProjectRole.OWNER, ProjectRole.MEMBER);
    }

    @Test
    void notificationTypeEnumMatchesSpecification() {
        assertThat(NotificationType.values())
                .containsExactly(
                        NotificationType.PROJECT_INVITED,
                        NotificationType.INVITATION_ACCEPTED,
                        NotificationType.TASK_ASSIGNED,
                        NotificationType.COMMENT_CREATED,
                        NotificationType.COMMENT_MENTIONED
                );
    }

    @Test
    void invitationStatusEnumMatchesSpecification() {
        assertThat(InvitationStatus.values())
                .containsExactly(
                        InvitationStatus.PENDING,
                        InvitationStatus.ACCEPTED,
                        InvitationStatus.REJECTED,
                        InvitationStatus.CANCELED,
                        InvitationStatus.EXPIRED
                );
    }

    @Test
    void notificationReferenceTypeEnumMatchesSpecification() {
        assertThat(NotificationReferenceType.values())
                .containsExactly(
                        NotificationReferenceType.PROJECT,
                        NotificationReferenceType.TASK,
                        NotificationReferenceType.INVITATION,
                        NotificationReferenceType.WORKSPACE_INVITATION,
                        NotificationReferenceType.COMMENT
                );
    }

    @Test
    void roleAndUserStatusEnumMatchSpecification() {
        assertThat(Role.values()).containsExactly(Role.ROLE_USER, Role.ROLE_ADMIN);
        assertThat(UserStatus.values()).containsExactly(UserStatus.ACTIVE, UserStatus.LOCKED, UserStatus.DELETED);
    }

    @Test
    void oauthProviderEnumMatchesSpecification() {
        assertThat(OAuthProvider.values())
                .containsExactly(OAuthProvider.GOOGLE, OAuthProvider.NAVER);
    }

    @Test
    void pushPlatformEnumMatchesSpecification() {
        assertThat(PushPlatform.values())
                .containsExactly(PushPlatform.WEB, PushPlatform.ANDROID, PushPlatform.IOS);
    }

    @Test
    void workspaceRoleEnumMatchesSpecification() {
        assertThat(WorkspaceRole.values())
                .containsExactly(WorkspaceRole.OWNER, WorkspaceRole.MEMBER);
    }
}
