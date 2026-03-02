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
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final LabelRepository labelRepository;
    private final TaskLabelRepository taskLabelRepository;

    @Transactional
    public LabelResponse createLabel(Long userId, Long projectId, CreateLabelRequest request) {
        Project project = findActiveProject(projectId);
        findMembership(projectId, userId);
        validateDuplicatedLabelName(projectId, request.name(), null);

        Label saved = labelRepository.save(Label.builder()
                .project(project)
                .name(request.name())
                .colorHex(request.colorHex())
                .build());

        project.touch(LocalDateTime.now());
        return toResponse(saved);
    }

    public List<LabelResponse> getLabels(Long userId, Long projectId) {
        findActiveProject(projectId);
        findMembership(projectId, userId);

        return labelRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LabelResponse updateLabel(Long userId, Long labelId, UpdateLabelRequest request) {
        Label label = findLabel(labelId);
        Long projectId = label.getProject().getId();
        findMembership(projectId, userId);
        validateDuplicatedLabelName(projectId, request.name(), labelId);

        label.update(request.name(), request.colorHex());
        label.getProject().touch(LocalDateTime.now());

        return toResponse(label);
    }

    @Transactional
    public void deleteLabel(Long userId, Long labelId) {
        Label label = findLabel(labelId);
        Long projectId = label.getProject().getId();
        findMembership(projectId, userId);

        taskLabelRepository.deleteAllByLabelId(labelId);
        labelRepository.delete(label);
        label.getProject().touch(LocalDateTime.now());
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private Label findLabel(Long labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LABEL_NOT_FOUND));
    }

    private void validateDuplicatedLabelName(Long projectId, String name, Long excludeId) {
        boolean exists = excludeId == null
                ? labelRepository.existsByProjectIdAndName(projectId, name)
                : labelRepository.existsByProjectIdAndNameAndIdNot(projectId, name, excludeId);

        if (exists) {
            throw new BusinessException(ErrorCode.LABEL_NAME_DUPLICATE);
        }
    }

    private LabelResponse toResponse(Label label) {
        return new LabelResponse(
                label.getId(),
                label.getName(),
                label.getColorHex()
        );
    }
}
