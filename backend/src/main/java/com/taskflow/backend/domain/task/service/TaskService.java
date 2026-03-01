package com.taskflow.backend.domain.task.service;

import com.taskflow.backend.domain.label.entity.Label;
import com.taskflow.backend.domain.label.repository.LabelRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.dto.request.CreateTaskRequest;
import com.taskflow.backend.domain.task.dto.response.TaskSummaryResponse;
import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.task.entity.TaskLabel;
import com.taskflow.backend.domain.task.repository.TaskLabelRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskLabelRepository taskLabelRepository;
    private final LabelRepository labelRepository;

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

        return toTaskSummaryResponse(savedTask);
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private User resolveAssignee(Long projectId, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return null;
        }
        return projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeUserId)
                .map(ProjectMember::getUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
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
}
