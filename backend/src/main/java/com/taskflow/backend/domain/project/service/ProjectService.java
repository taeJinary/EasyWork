package com.taskflow.backend.domain.project.service;

import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectDetailResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListItemResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.ProjectRole;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectSummaryResponse createProject(Long userId, CreateProjectRequest request) {
        User owner = findActiveUser(userId);

        Project project = Project.builder()
                .owner(owner)
                .name(request.name())
                .description(request.description())
                .build();

        Project savedProject = projectRepository.save(project);
        projectMemberRepository.save(
                ProjectMember.create(savedProject, owner, ProjectRole.OWNER, LocalDateTime.now())
        );

        return new ProjectSummaryResponse(
                savedProject.getId(),
                savedProject.getName(),
                savedProject.getDescription(),
                ProjectRole.OWNER
        );
    }

    public ProjectListResponse getMyProjects(Long userId, int page, int size) {
        findActiveUser(userId);

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size > 0 ? size : 20;

        List<ProjectMember> memberships = projectMemberRepository.findAllActiveByUserIdOrderByProjectUpdatedAtDesc(userId);
        long totalElements = memberships.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);

        int fromIndex = Math.min(normalizedPage * normalizedSize, memberships.size());
        int toIndex = Math.min(fromIndex + normalizedSize, memberships.size());

        List<ProjectListItemResponse> content = memberships.subList(fromIndex, toIndex).stream()
                .map(this::toProjectListItem)
                .toList();

        boolean first = normalizedPage == 0;
        boolean last = totalPages == 0 || normalizedPage >= totalPages - 1;

        return new ProjectListResponse(
                content,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                first,
                last
        );
    }

    public ProjectDetailResponse getProjectDetail(Long userId, Long projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        ProjectMember myMembership = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));

        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId);

        List<ProjectDetailResponse.MemberResponse> memberResponses = members.stream()
                .map(member -> new ProjectDetailResponse.MemberResponse(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getEmail(),
                        member.getUser().getNickname(),
                        member.getRole(),
                        member.getJoinedAt()
                ))
                .toList();

        return new ProjectDetailResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                myMembership.getRole(),
                (long) members.size(),
                0L,
                new ProjectDetailResponse.TaskSummaryResponse(0L, 0L, 0L),
                memberResponses
        );
    }

    private ProjectListItemResponse toProjectListItem(ProjectMember projectMember) {
        Project project = projectMember.getProject();
        long memberCount = projectMemberRepository.countByProjectId(project.getId());

        return new ProjectListItemResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                projectMember.getRole(),
                memberCount,
                0L,
                0L,
                0,
                project.getUpdatedAt()
        );
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}

