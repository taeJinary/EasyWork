package com.taskflow.backend.domain.comment.service;

import com.taskflow.backend.domain.comment.dto.request.CreateCommentRequest;
import com.taskflow.backend.domain.comment.dto.response.CommentListResponse;
import com.taskflow.backend.domain.comment.dto.response.CommentResponse;
import com.taskflow.backend.domain.comment.entity.Comment;
import com.taskflow.backend.domain.comment.repository.CommentRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.Role;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createCommentCreatesCommentForProjectMember() {
        User actor = activeUser(1L, "member@example.com", "member");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("task")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .position(0)
                .version(0L)
                .build();
        Comment savedComment = Comment.create(task, actor, "로그인 실패");
        ReflectionTestUtils.setField(savedComment, "id", 150L);
        ReflectionTestUtils.setField(savedComment, "createdAt", LocalDateTime.of(2026, 3, 2, 10, 0));
        ReflectionTestUtils.setField(savedComment, "updatedAt", LocalDateTime.of(2026, 3, 2, 10, 0));
        LocalDateTime beforeActivityAt = LocalDateTime.of(2026, 3, 1, 8, 0);
        ReflectionTestUtils.setField(project, "updatedAt", beforeActivityAt);

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

        CommentResponse response = commentService.createComment(1L, 1000L, new CreateCommentRequest("로그인 실패"));

        assertThat(response.commentId()).isEqualTo(150L);
        assertThat(response.author().userId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("로그인 실패");
        assertThat(response.editable()).isTrue();
        assertThat(project.getUpdatedAt()).isAfter(beforeActivityAt);
    }

    @Test
    void createCommentThrowsWhenTaskNotFound() {
        given(taskRepository.findByIdAndDeletedAtIsNull(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(1L, 9999L, new CreateCommentRequest("x")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @Test
    void createCommentThrowsWhenNotProjectMember() {
        User actor = activeUser(1L, "member@example.com", "member");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("task")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .position(0)
                .version(0L)
                .build();

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(1L, 1000L, new CreateCommentRequest("x")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_PROJECT_MEMBER);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getCommentsReturnsCursorPage() {
        User actor = activeUser(1L, "member@example.com", "member");
        User other = activeUser(2L, "other@example.com", "other");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("task")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .position(0)
                .version(0L)
                .build();
        Comment c150 = Comment.create(task, actor, "c150");
        Comment c149 = Comment.create(task, other, "c149");
        Comment c148 = Comment.create(task, other, "c148");
        ReflectionTestUtils.setField(c150, "id", 150L);
        ReflectionTestUtils.setField(c149, "id", 149L);
        ReflectionTestUtils.setField(c148, "id", 148L);
        ReflectionTestUtils.setField(c150, "createdAt", LocalDateTime.of(2026, 3, 2, 10, 0));
        ReflectionTestUtils.setField(c149, "createdAt", LocalDateTime.of(2026, 3, 2, 9, 59));
        ReflectionTestUtils.setField(c148, "createdAt", LocalDateTime.of(2026, 3, 2, 9, 58));
        ReflectionTestUtils.setField(c150, "updatedAt", LocalDateTime.of(2026, 3, 2, 10, 0));
        ReflectionTestUtils.setField(c149, "updatedAt", LocalDateTime.of(2026, 3, 2, 9, 59));
        ReflectionTestUtils.setField(c148, "updatedAt", LocalDateTime.of(2026, 3, 2, 9, 58));

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(commentRepository.findByTaskIdAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any(Pageable.class)))
                .willReturn(List.of(c150, c149, c148));

        CommentListResponse response = commentService.getComments(1L, 1000L, 160L, 2);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).commentId()).isEqualTo(150L);
        assertThat(response.content().get(0).editable()).isTrue();
        assertThat(response.content().get(1).commentId()).isEqualTo(149L);
        assertThat(response.content().get(1).editable()).isFalse();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(149L);
    }

    @Test
    void getCommentsReturnsFirstPageWhenCursorMissing() {
        User actor = activeUser(1L, "member@example.com", "member");
        Project project = Project.builder()
                .id(10L)
                .owner(actor)
                .name("TaskFlow")
                .description("desc")
                .build();
        ProjectMember membership = ProjectMember.builder()
                .id(100L)
                .project(project)
                .user(actor)
                .role(ProjectRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build();
        Task task = Task.builder()
                .id(1000L)
                .project(project)
                .creator(actor)
                .assignee(null)
                .title("task")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .position(0)
                .version(0L)
                .build();
        Comment c150 = Comment.create(task, actor, "c150");
        ReflectionTestUtils.setField(c150, "id", 150L);
        ReflectionTestUtils.setField(c150, "createdAt", LocalDateTime.of(2026, 3, 2, 10, 0));
        ReflectionTestUtils.setField(c150, "updatedAt", LocalDateTime.of(2026, 3, 2, 10, 0));

        given(taskRepository.findByIdAndDeletedAtIsNull(1000L)).willReturn(Optional.of(task));
        given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(membership));
        given(commentRepository.findByTaskIdOrderByIdDesc(anyLong(), any(Pageable.class)))
                .willReturn(List.of(c150));

        CommentListResponse response = commentService.getComments(1L, 1000L, null, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().commentId()).isEqualTo(150L);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
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
}
