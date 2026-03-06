package com.taskflow.backend.domain;

import com.taskflow.backend.domain.attachment.controller.AttachmentHttpContract;
import com.taskflow.backend.domain.comment.controller.CommentHttpContract;
import com.taskflow.backend.domain.invitation.controller.InvitationHttpContract;
import com.taskflow.backend.domain.notification.controller.NotificationHttpContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollaborationHttpContractTest {

    @Test
    void invitationPathsRemainFrozen() {
        assertThat(InvitationHttpContract.PROJECT_INVITATIONS_PATH).isEqualTo("/projects/{projectId}/invitations");
        assertThat(InvitationHttpContract.MY_INVITATIONS_PATH).isEqualTo("/invitations/me");
        assertThat(InvitationHttpContract.ACCEPT_PATH).isEqualTo("/invitations/{invitationId}/accept");
        assertThat(InvitationHttpContract.REJECT_PATH).isEqualTo("/invitations/{invitationId}/reject");
        assertThat(InvitationHttpContract.CANCEL_PATH)
                .isEqualTo("/projects/{projectId}/invitations/{invitationId}/cancel");
    }

    @Test
    void notificationPathsRemainFrozen() {
        assertThat(NotificationHttpContract.BASE_PATH).isEqualTo("/notifications");
        assertThat(NotificationHttpContract.UNREAD_COUNT_PATH).isEqualTo("/unread-count");
        assertThat(NotificationHttpContract.READ_PATH).isEqualTo("/{notificationId}/read");
        assertThat(NotificationHttpContract.READ_ALL_PATH).isEqualTo("/read-all");
    }

    @Test
    void attachmentPathsRemainFrozen() {
        assertThat(AttachmentHttpContract.TASK_ATTACHMENTS_PATH).isEqualTo("/tasks/{taskId}/attachments");
        assertThat(AttachmentHttpContract.ATTACHMENT_PATH).isEqualTo("/attachments/{attachmentId}");
    }

    @Test
    void commentPathsRemainFrozen() {
        assertThat(CommentHttpContract.TASK_COMMENTS_PATH).isEqualTo("/tasks/{taskId}/comments");
        assertThat(CommentHttpContract.COMMENT_PATH).isEqualTo("/comments/{commentId}");
    }
}
