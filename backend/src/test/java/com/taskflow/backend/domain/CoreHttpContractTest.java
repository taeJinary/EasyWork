package com.taskflow.backend.domain;

import com.taskflow.backend.domain.project.controller.ProjectHttpContract;
import com.taskflow.backend.domain.task.controller.TaskHttpContract;
import com.taskflow.backend.domain.workspace.controller.WorkspaceHttpContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreHttpContractTest {

    @Test
    void workspacePathsRemainFrozen() {
        assertThat(WorkspaceHttpContract.BASE_PATH).isEqualTo("/workspaces");
        assertThat(WorkspaceHttpContract.DETAIL_PATH).isEqualTo("/{workspaceId}");
        assertThat(WorkspaceHttpContract.MEMBERS_PATH).isEqualTo("/{workspaceId}/members");
    }

    @Test
    void projectPathsRemainFrozen() {
        assertThat(ProjectHttpContract.BASE_PATH).isEqualTo("/projects");
        assertThat(ProjectHttpContract.DETAIL_PATH).isEqualTo("/{projectId}");
        assertThat(ProjectHttpContract.MEMBERS_PATH).isEqualTo("/{projectId}/members");
        assertThat(ProjectHttpContract.MEMBER_ROLE_PATH).isEqualTo("/{projectId}/members/{memberId}/role");
        assertThat(ProjectHttpContract.MEMBER_PATH).isEqualTo("/{projectId}/members/{memberId}");
    }

    @Test
    void taskPathsRemainFrozen() {
        assertThat(TaskHttpContract.PROJECT_TASKS_PATH).isEqualTo("/projects/{projectId}/tasks");
        assertThat(TaskHttpContract.PROJECT_TASK_BOARD_PATH).isEqualTo("/projects/{projectId}/tasks/board");
        assertThat(TaskHttpContract.TASK_DETAIL_PATH).isEqualTo("/tasks/{taskId}");
        assertThat(TaskHttpContract.TASK_MOVE_PATH).isEqualTo("/tasks/{taskId}/move");
    }
}
