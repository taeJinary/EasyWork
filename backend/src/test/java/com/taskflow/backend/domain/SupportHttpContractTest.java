package com.taskflow.backend.domain;

import com.taskflow.backend.domain.dashboard.controller.DashboardHttpContract;
import com.taskflow.backend.domain.label.controller.LabelHttpContract;
import com.taskflow.backend.domain.notification.controller.NotificationPushTokenHttpContract;
import com.taskflow.backend.domain.user.controller.UserHttpContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportHttpContractTest {

    @Test
    void labelPathsRemainFrozen() {
        assertThat(LabelHttpContract.PROJECT_LABELS_PATH).isEqualTo("/projects/{projectId}/labels");
        assertThat(LabelHttpContract.LABEL_PATH).isEqualTo("/labels/{labelId}");
    }

    @Test
    void dashboardPathsRemainFrozen() {
        assertThat(DashboardHttpContract.PROJECTS_PATH).isEqualTo("/dashboard/projects");
        assertThat(DashboardHttpContract.PROJECT_DASHBOARD_PATH).isEqualTo("/projects/{projectId}/dashboard");
    }

    @Test
    void userPathsRemainFrozen() {
        assertThat(UserHttpContract.BASE_PATH).isEqualTo("/users");
        assertThat(UserHttpContract.ME_PATH).isEqualTo("/me");
        assertThat(UserHttpContract.ME_PASSWORD_PATH).isEqualTo("/me/password");
    }

    @Test
    void pushTokenPathsRemainFrozen() {
        assertThat(NotificationPushTokenHttpContract.BASE_PATH).isEqualTo("/notifications/push-tokens");
    }
}
