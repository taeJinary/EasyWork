package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.comment.repository.CommentRepository;
import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.label.repository.LabelRepository;
import com.taskflow.backend.domain.notification.service.NotificationService;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.request.MoveTaskRequest;
import com.taskflow.backend.domain.task.dto.request.UpdateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskBoardResponse;
import com.taskflow.backend.domain.task.dto.response.TaskDetailResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListItemResponse;
import com.taskflow.backend.domain.task.dto.response.TaskListResponse;
import com.taskflow.backend.domain.task.dto.response.TaskMoveResponse;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.entity.TaskLabel;
import com.taskflow.backend.domain.task.entity.TaskStatusHistory;
import com.taskflow.backend.domain.task.repository.TaskLabelRepository;
import com.taskflow.backend.domain.task.repository.TaskQueryRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.task.repository.TaskStatusHistoryRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import com.taskflow.backend.global.websocket.ProjectBoardEventPublisher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskQueryRepository taskQueryRepository;
    private final TaskLabelRepository taskLabelRepository;
    private final CommentRepository commentRepository;
    private final LabelRepository labelRepository;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final NotificationService notificationService;
    private final ProjectBoardEventPublisher projectBoardEventPublisher;

    @Transactional
    public TaskSummaryResponse createTask(Long userId, Long projectId, CreateTaskRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        ProjectMember creatorMembership = findMembership(projectId, userId);
        User assignee = resolveAssignee(projectId, request.assigneeUserId());
        TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
        int position = (int) taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.TODO);

        User creator = creatorMembership.getUser();
        Task savedTask = taskRepository.save(Task.create(
                project,
                creator,
                assignee,
                request.title(),
                request.description(),
                priority,
                request.dueDate(),
                position
        ));

        syncTaskLabels(savedTask, projectId, request.labelIds());
        project.touch(LocalDateTime.now());
        notificationService.createTaskAssignedNotification(savedTask, creator);
        projectBoardEventPublisher.publishTaskCreated(savedTask, creator);

        return toTaskSummaryResponse(savedTask);
    }

    @Transactional
    public TaskDetailResponse updateTask(Long userId, Long taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        Long projectId = task.getProject().getId();
        ProjectMember actorMembership = findMembership(projectId, userId);
        validateTaskVersion(task, request.version());

        User previousAssignee = task.getAssignee();
        User assignee = resolveAssignee(projectId, request.assigneeUserId());
        task.update(
                request.title(),
                request.description(),
                assignee,
                request.priority(),
                request.dueDate()
        );

        replaceTaskLabels(task, projectId, request.labelIds());
        task.getProject().touch(LocalDateTime.now());
        if (isAssigneeChanged(previousAssignee, assignee)) {
            notificationService.createTaskAssignedNotification(task, actorMembership.getUser());
        }
        projectBoardEventPublisher.publishTaskUpdated(task, actorMembership.getUser());

        return getTaskDetail(userId, taskId);
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        Long projectId = task.getProject().getId();
        ProjectMember membership = findMembership(projectId, userId);
        boolean isCreator = task.getCreator().getId().equals(userId);
        boolean isOwner = membership.getRole() == ProjectRole.OWNER;

        if (!isCreator && !isOwner) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        task.delete(LocalDateTime.now());
        task.getProject().touch(LocalDateTime.now());
        projectBoardEventPublisher.publishTaskDeleted(task, membership.getUser());
    }

    @Transactional
    public TaskMoveResponse moveTask(Long userId, Long taskId, MoveTaskRequest request) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        Long projectId = task.getProject().getId();
        ProjectMember actorMembership = findMembership(projectId, userId);
        validateTaskVersion(task, request.version());

        TaskStatus fromStatus = task.getStatus();
        TaskStatus toStatus = request.toStatus();
        int targetPosition = request.targetPosition();

        if (fromStatus == toStatus) {
            moveWithinSameColumn(task, projectId, fromStatus, targetPosition);
        } else {
            moveAcrossColumns(task, projectId, fromStatus, toStatus, targetPosition);
            taskStatusHistoryRepository.save(TaskStatusHistory.create(
                    task,
                    fromStatus,
                    toStatus,
                    actorMembership.getUser()
            ));
        }

        task.getProject().touch(LocalDateTime.now());
        taskRepository.flush();
        projectBoardEventPublisher.publishTaskMoved(task, actorMembership.getUser(), fromStatus, toStatus);
        return toTaskMoveResponse(task);
    }

    public TaskDetailResponse getTaskDetail(Long userId, Long taskId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        findMembership(task.getProject().getId(), userId);
        List<TaskStatusHistory> statusHistories = taskStatusHistoryRepository
                .findTop10ByTaskIdOrderByCreatedAtDesc(taskId);

        List<TaskBoardResponse.LabelResponse> boardLabels = mapLabelsByTaskId(List.of(task)).getOrDefault(taskId, List.of());
        long commentCount = mapCommentCountsByTaskId(List.of(task)).getOrDefault(taskId, 0L);
        List<TaskDetailResponse.LabelResponse> labels = boardLabels.stream()
                .map(label -> new TaskDetailResponse.LabelResponse(
                        label.labelId(),
                        label.name(),
                        label.colorHex()
                ))
                .toList();

        return toTaskDetailResponse(task, labels, commentCount, statusHistories == null ? List.of() : statusHistories);
    }

    public TaskBoardResponse getTaskBoard(
            Long userId,
            Long projectId,
            Long assigneeUserId,
            TaskPriority priority,
            Long labelId,
            String keyword
    ) {
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        findMembership(projectId, userId);

        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        List<Task> filteredTasks = taskQueryRepository.findTaskBoardTasks(
                projectId,
                assigneeUserId,
                priority,
                labelId,
                normalizedKeyword
        );
        Map<Long, List<TaskBoardResponse.LabelResponse>> labelsByTaskId = mapLabelsByTaskId(filteredTasks);
        Map<Long, Long> commentCountsByTaskId = mapCommentCountsByTaskId(filteredTasks);

        Map<TaskStatus, List<TaskBoardResponse.TaskCardResponse>> cardsByStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            cardsByStatus.put(status, new ArrayList<>());
        }

        for (Task task : filteredTasks) {
            List<TaskBoardResponse.LabelResponse> labels = labelsByTaskId.getOrDefault(task.getId(), List.of());
            long commentCount = commentCountsByTaskId.getOrDefault(task.getId(), 0L);
            cardsByStatus.get(task.getStatus()).add(toTaskCardResponse(task, labels, commentCount));
        }

        List<TaskBoardResponse.ColumnResponse> columns = java.util.Arrays.stream(TaskStatus.values())
                .map(status -> new TaskBoardResponse.ColumnResponse(status, cardsByStatus.get(status)))
                .toList();

        return new TaskBoardResponse(
                projectId,
                new TaskBoardResponse.FilterResponse(assigneeUserId, priority, labelId, normalizedKeyword),
                columns
        );
    }

    public TaskListResponse getTasks(
            Long userId,
            Long projectId,
            int page,
            int size,
            TaskStatus status,
            String sortBy,
            String direction,
            String keyword
    ) {
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        findMembership(projectId, userId);

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? size : 20;
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        PageRequest pageRequest = PageRequest.of(normalizedPage, normalizedSize);
        Page<Task> taskPage = taskQueryRepository.findTasks(
                projectId,
                status,
                sortBy,
                direction,
                normalizedKeyword,
                pageRequest
        );
        Map<Long, Long> commentCountsByTaskId = mapCommentCountsByTaskId(taskPage.getContent());

        List<TaskListItemResponse> content = taskPage.getContent().stream()
                .map(task -> toTaskListItemResponse(task, commentCountsByTaskId.getOrDefault(task.getId(), 0L)))
                .toList();

        int totalPages = taskPage.getTotalPages();
        boolean first = taskPage.isFirst();
        boolean last = totalPages == 0 || taskPage.isLast();

        return new TaskListResponse(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                totalPages,
                first,
                last
        );
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private void moveWithinSameColumn(
            Task movingTask,
            Long projectId,
            TaskStatus status,
            int targetPosition
    ) {
        List<Task> columnTasks = new ArrayList<>(
                taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(projectId, status)
        );
        columnTasks.removeIf(task -> task.getId().equals(movingTask.getId()));

        int normalizedTargetPosition = normalizeTargetPosition(targetPosition, columnTasks.size());
        columnTasks.add(normalizedTargetPosition, movingTask);
        reassignPositions(columnTasks);
    }

    private void moveAcrossColumns(
            Task movingTask,
            Long projectId,
            TaskStatus fromStatus,
            TaskStatus toStatus,
            int targetPosition
    ) {
        List<Task> sourceTasks = new ArrayList<>(
                taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(projectId, fromStatus)
        );
        sourceTasks.removeIf(task -> task.getId().equals(movingTask.getId()));
        reassignPositions(sourceTasks);

        List<Task> targetTasks = new ArrayList<>(
                taskRepository.findAllByProjectIdAndStatusAndDeletedAtIsNullOrderByPositionAsc(projectId, toStatus)
        );
        int normalizedTargetPosition = normalizeTargetPosition(targetPosition, targetTasks.size());

        movingTask.move(toStatus, normalizedTargetPosition, LocalDateTime.now());
        targetTasks.add(normalizedTargetPosition, movingTask);
        reassignPositions(targetTasks);
    }

    private int normalizeTargetPosition(int targetPosition, int columnSize) {
        if (targetPosition < 0) {
            return 0;
        }
        return Math.min(targetPosition, columnSize);
    }

    private void reassignPositions(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).reassignPosition(i);
        }
    }

    private User resolveAssignee(Long projectId, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return null;
        }
        return projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeUserId)
                .map(ProjectMember::getUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private boolean isAssigneeChanged(User previousAssignee, User currentAssignee) {
        Long previousAssigneeId = previousAssignee == null ? null : previousAssignee.getId();
        Long currentAssigneeId = currentAssignee == null ? null : currentAssignee.getId();
        return !java.util.Objects.equals(previousAssigneeId, currentAssigneeId);
    }

    private void syncTaskLabels(Task task, Long projectId, List<Long> labelIds) {
        List<Long> requestedLabelIds = labelIds == null ? Collections.emptyList() : labelIds;
        if (requestedLabelIds.isEmpty()) {
            return;
        }

        List<Label> labels = labelRepository.findAllByIdInAndProjectId(requestedLabelIds, projectId);
        if (labels.size() != requestedLabelIds.size()) {
            throw new BusinessException(ErrorCode.LABEL_NOT_FOUND);
        }

        List<TaskLabel> taskLabels = labels.stream()
                .map(label -> TaskLabel.create(task, label))
                .toList();
        taskLabelRepository.saveAll(taskLabels);
    }

    private void replaceTaskLabels(Task task, Long projectId, List<Long> labelIds) {
        List<Long> requestedLabelIds = normalizeLabelIds(labelIds);
        if (requestedLabelIds.isEmpty()) {
            taskLabelRepository.deleteAllByTaskId(task.getId());
            return;
        }

        List<Label> labels = labelRepository.findAllByIdInAndProjectId(requestedLabelIds, projectId);
        if (labels.size() != requestedLabelIds.size()) {
            throw new BusinessException(ErrorCode.LABEL_NOT_FOUND);
        }

        taskLabelRepository.deleteAllByTaskId(task.getId());
        List<TaskLabel> taskLabels = labels.stream()
                .map(label -> TaskLabel.create(task, label))
                .toList();
        taskLabelRepository.saveAll(taskLabels);
    }

    private List<Long> normalizeLabelIds(List<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(labelIds));
    }

    private void validateTaskVersion(Task task, Long requestedVersion) {
        if (task.getVersion() == null || !task.getVersion().equals(requestedVersion)) {
            throw new BusinessException(ErrorCode.TASK_CONFLICT);
        }
    }

    private Map<Long, List<TaskBoardResponse.LabelResponse>> mapLabelsByTaskId(List<Task> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(Task::getId)
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        List<TaskLabel> taskLabels = taskLabelRepository.findAllByTaskIdInWithLabel(taskIds);
        return taskLabels.stream().collect(
                java.util.stream.Collectors.groupingBy(
                        taskLabel -> taskLabel.getTask().getId(),
                        java.util.stream.Collectors.mapping(
                                taskLabel -> new TaskBoardResponse.LabelResponse(
                                        taskLabel.getLabel().getId(),
                                        taskLabel.getLabel().getName(),
                                        taskLabel.getLabel().getColorHex()
                                ),
                                java.util.stream.Collectors.toList()
                        )
                )
        );
    }

    private Map<Long, Long> mapCommentCountsByTaskId(List<Task> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(Task::getId)
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        List<CommentRepository.TaskCommentCountProjection> commentCounts = commentRepository.countByTaskIdIn(taskIds);
        if (commentCounts == null || commentCounts.isEmpty()) {
            return Map.of();
        }

        return commentCounts.stream().collect(
                java.util.stream.Collectors.toMap(
                        CommentRepository.TaskCommentCountProjection::getTaskId,
                        CommentRepository.TaskCommentCountProjection::getCommentCount
                )
        );
    }

    private TaskBoardResponse.TaskCardResponse toTaskCardResponse(
            Task task,
            List<TaskBoardResponse.LabelResponse> labels,
            Long commentCount
    ) {
        TaskBoardResponse.AssigneeResponse assigneeResponse = task.getAssignee() == null
                ? null
                : new TaskBoardResponse.AssigneeResponse(
                        task.getAssignee().getId(),
                        task.getAssignee().getNickname()
                );

        return new TaskBoardResponse.TaskCardResponse(
                task.getId(),
                task.getTitle(),
                task.getPriority(),
                task.getDueDate(),
                task.getPosition(),
                task.getVersion(),
                assigneeResponse,
                labels,
                commentCount
        );
    }

    private TaskListItemResponse toTaskListItemResponse(Task task, Long commentCount) {
        TaskListItemResponse.AssigneeResponse assigneeResponse = task.getAssignee() == null
                ? null
                : new TaskListItemResponse.AssigneeResponse(
                        task.getAssignee().getId(),
                        task.getAssignee().getNickname()
                );

        return new TaskListItemResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getPosition(),
                task.getVersion(),
                commentCount,
                assigneeResponse
        );
    }

    private TaskSummaryResponse toTaskSummaryResponse(Task task) {
        TaskSummaryResponse.AssigneeResponse assigneeResponse = task.getAssignee() == null
                ? null
                : new TaskSummaryResponse.AssigneeResponse(
                        task.getAssignee().getId(),
                        task.getAssignee().getNickname()
                );

        return new TaskSummaryResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getPosition(),
                task.getVersion(),
                assigneeResponse
        );
    }

    private TaskMoveResponse toTaskMoveResponse(Task task) {
        return new TaskMoveResponse(
                task.getId(),
                task.getStatus(),
                task.getPosition(),
                task.getVersion(),
                task.getCompletedAt()
        );
    }

    private TaskDetailResponse toTaskDetailResponse(
            Task task,
            List<TaskDetailResponse.LabelResponse> labels,
            Long commentCount,
            List<TaskStatusHistory> statusHistories
    ) {
        TaskDetailResponse.UserSummaryResponse creator = new TaskDetailResponse.UserSummaryResponse(
                task.getCreator().getId(),
                task.getCreator().getNickname()
        );
        TaskDetailResponse.UserSummaryResponse assignee = task.getAssignee() == null
                ? null
                : new TaskDetailResponse.UserSummaryResponse(
                        task.getAssignee().getId(),
                        task.getAssignee().getNickname()
                );

        return new TaskDetailResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getPosition(),
                task.getVersion(),
                creator,
                assignee,
                labels,
                commentCount,
                toStatusHistoryResponses(statusHistories),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private List<TaskDetailResponse.StatusHistoryResponse> toStatusHistoryResponses(
            List<TaskStatusHistory> statusHistories
    ) {
        if (statusHistories == null || statusHistories.isEmpty()) {
            return List.of();
        }
        return statusHistories.stream()
                .map(history -> new TaskDetailResponse.StatusHistoryResponse(
                        history.getId(),
                        history.getFromStatus(),
                        history.getToStatus(),
                        new TaskDetailResponse.UserSummaryResponse(
                                history.getChangedBy().getId(),
                                history.getChangedBy().getNickname()
                        ),
                        history.getCreatedAt()
                ))
                .toList();
    }
}
