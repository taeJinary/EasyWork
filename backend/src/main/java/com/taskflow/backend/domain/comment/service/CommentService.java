package com.taskflow.backend.domain.comment.service;

import com.taskflow.backend.domain.comment.dto.request.CreateCommentRequest;
import com.taskflow.backend.domain.comment.dto.request.UpdateCommentRequest;
import com.taskflow.backend.domain.comment.dto.response.CommentListResponse;
import com.taskflow.backend.domain.comment.dto.response.CommentResponse;
import com.taskflow.backend.domain.comment.entity.Comment;
import com.taskflow.backend.domain.comment.repository.CommentRepository;
import com.taskflow.backend.domain.notification.service.NotificationService;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.websocket.ProjectBoardEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int DEFAULT_SIZE = 20;

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final ProjectBoardEventPublisher projectBoardEventPublisher;

    @Transactional
    public CommentResponse createComment(Long userId, Long taskId, CreateCommentRequest request) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        ProjectMember membership = findMembership(task.getProject().getId(), userId);

        Comment saved = commentRepository.save(Comment.create(
                task,
                membership.getUser(),
                request.content()
        ));
        task.getProject().touch(LocalDateTime.now());
        Set<Long> mentionedUserIds = notificationService.createCommentMentionNotifications(saved, membership.getUser());
        notificationService.createCommentCreatedNotification(saved, membership.getUser(), mentionedUserIds);
        projectBoardEventPublisher.publishCommentCreated(saved, membership.getUser());
        return toCommentResponse(saved, userId);
    }

    public CommentListResponse getComments(Long userId, Long taskId, Long cursor, int size) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        findMembership(task.getProject().getId(), userId);

        int normalizedSize = size > 0 ? size : DEFAULT_SIZE;
        Pageable pageable = PageRequest.of(0, normalizedSize + 1);
        List<Comment> fetched = cursor == null
                ? commentRepository.findByTaskIdOrderByIdDesc(taskId, pageable)
                : commentRepository.findByTaskIdAndIdLessThanOrderByIdDesc(taskId, cursor, pageable);

        boolean hasNext = fetched.size() > normalizedSize;
        List<Comment> pageContent = hasNext ? fetched.subList(0, normalizedSize) : fetched;
        Long nextCursor = hasNext && !pageContent.isEmpty() ? pageContent.getLast().getId() : null;

        List<CommentResponse> content = pageContent.stream()
                .map(comment -> toCommentResponse(comment, userId))
                .toList();

        return new CommentListResponse(content, nextCursor, hasNext);
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, UpdateCommentRequest request) {
        Comment comment = findComment(commentId);
        Long projectId = comment.getTask().getProject().getId();
        findMembership(projectId, userId);
        ensureAuthor(userId, comment);

        comment.updateContent(request.content());
        comment.getTask().getProject().touch(LocalDateTime.now());

        return toCommentResponse(comment, userId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = findComment(commentId);
        Long projectId = comment.getTask().getProject().getId();
        ProjectMember membership = findMembership(projectId, userId);
        ensureAuthorOrOwner(userId, membership, comment);

        commentRepository.delete(comment);
        comment.getTask().getProject().touch(LocalDateTime.now());
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void ensureAuthor(Long userId, Comment comment) {
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    private void ensureAuthorOrOwner(Long userId, ProjectMember membership, Comment comment) {
        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean isOwner = membership.getRole() == ProjectRole.OWNER;
        if (!isAuthor && !isOwner) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
    }

    private CommentResponse toCommentResponse(Comment comment, Long userId) {
        return new CommentResponse(
                comment.getId(),
                new CommentResponse.AuthorResponse(
                        comment.getAuthor().getId(),
                        comment.getAuthor().getNickname()
                ),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getAuthor().getId().equals(userId)
        );
    }
}
