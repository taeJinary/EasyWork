package com.taskflow.backend.domain.label.service;

import com.taskflow.backend.domain.label.dto.request.CreateLabelRequest;
import com.taskflow.backend.domain.label.dto.request.UpdateLabelRequest;
import com.taskflow.backend.domain.label.dto.response.LabelResponse;
import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.label.repository.LabelRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.repository.TaskLabelRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.UserStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private TaskLabelRepository taskLabelRepository;

    @InjectMocks
    private LabelService labelService;

    @Test
    void createLabelCreatesLabelWhenProjectMember() {
        User member = activeUser(1L, "member@example.com", "member");
        Project project = project(10L, member);
        ProjectMember membership = membership(100L, project, member, ProjectRole.MEMBER);
        Label saved = Label.builder()
                .id(200L)
                .project(project)
                .name("Backend")
                .colorHex("#2563EB")
                .build();
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 2, 10, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(labelRepository.existsByProjectIdAndName(10L, "Backend")).willReturn(false);
        given(labelRepository.save(any(Label.class))).willReturn(saved);

        LabelResponse response = labelService.createLabel(1L, 10L, new CreateLabelRequest("Backend", "#2563EB"));

        assertThat(response.labelId()).isEqualTo(200L);
        assertThat(response.name()).isEqualTo("Backend");
        assertThat(response.colorHex()).isEqualTo("#2563EB");
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
    }

    @Test
    void createLabelThrowsWhenNameDuplicated() {
        User member = activeUser(1L, "member@example.com", "member");
        Project project = project(10L, member);
        ProjectMember membership = membership(100L, project, member, ProjectRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(labelRepository.existsByProjectIdAndName(10L, "Backend")).willReturn(true);

        assertThatThrownBy(() -> labelService.createLabel(1L, 10L, new CreateLabelRequest("Backend", "#2563EB")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LABEL_NAME_DUPLICATE);

        verify(labelRepository, never()).save(any(Label.class));
    }

    @Test
    void getLabelsReturnsProjectLabels() {
        User member = activeUser(1L, "member@example.com", "member");
        Project project = project(10L, member);
        ProjectMember membership = membership(100L, project, member, ProjectRole.MEMBER);
        Label backend = Label.builder().id(1L).project(project).name("Backend").colorHex("#2563EB").build();
        Label auth = Label.builder().id(2L).project(project).name("Auth").colorHex("#16A34A").build();

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(labelRepository.findAllByProjectIdOrderByCreatedAtAsc(10L)).willReturn(List.of(backend, auth));

        List<LabelResponse> response = labelService.getLabels(1L, 10L);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().name()).isEqualTo("Backend");
    }

    @Test
    void updateLabelUpdatesNameAndColor() {
        User member = activeUser(1L, "member@example.com", "member");
        Project project = project(10L, member);
        ProjectMember membership = membership(100L, project, member, ProjectRole.MEMBER);
        Label label = Label.builder().id(1L).project(project).name("Backend").colorHex("#2563EB").build();

        given(labelRepository.findById(1L)).willReturn(Optional.of(label));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(labelRepository.existsByProjectIdAndNameAndIdNot(10L, "Auth", 1L)).willReturn(false);

        LabelResponse response = labelService.updateLabel(1L, 1L, new UpdateLabelRequest("Auth", "#16A34A"));

        assertThat(response.name()).isEqualTo("Auth");
        assertThat(response.colorHex()).isEqualTo("#16A34A");
    }

    @Test
    void deleteLabelDeletesLabelAndMappings() {
        User member = activeUser(1L, "member@example.com", "member");
        Project project = project(10L, member);
        ProjectMember membership = membership(100L, project, member, ProjectRole.MEMBER);
        Label label = Label.builder().id(1L).project(project).name("Backend").colorHex("#2563EB").build();

        given(labelRepository.findById(1L)).willReturn(Optional.of(label));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));

        labelService.deleteLabel(1L, 1L);

        verify(taskLabelRepository).deleteAllByLabelId(1L);
        verify(labelRepository).delete(label);
    }

    @Test
    void deleteLabelThrowsWhenNotProjectMember() {
        User owner = activeUser(1L, "owner@example.com", "owner");
        Project project = project(10L, owner);
        Label label = Label.builder().id(1L).project(project).name("Backend").colorHex("#2563EB").build();

        given(labelRepository.findById(1L)).willReturn(Optional.of(label));
        given(projectMemberRepository.findByProjectIdAndUserId(anyLong(), anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> labelService.deleteLabel(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);

        verify(labelRepository, never()).delete(any(Label.class));
    }

    private User activeUser(Long id, String email, String nickname) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .nickname(nickname)
                .provider("LOCAL")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Project project(Long id, User owner) {
        return Project.builder()
                .id(id)
                .owner(owner)
                .name("TaskFlow")
                .description("desc")
                .build();
    }

    private ProjectMember membership(Long id, Project project, User user, ProjectRole role) {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
    }
}
