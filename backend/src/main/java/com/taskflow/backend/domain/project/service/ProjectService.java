package com.taskflow.backend.domain.project.service;

import com.taskflow.backend.domain.project.dto.request.CreateProjectRequest;
import com.taskflow.backend.domain.project.dto.request.ChangeMemberRoleRequest;
import com.taskflow.backend.domain.project.dto.request.UpdateProjectRequest;
import com.taskflow.backend.domain.project.dto.response.ProjectDetailResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListItemResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectListResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectMemberResponse;
import com.taskflow.backend.domain.project.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.ProjectRole;
import com.taskflow.backend.global.common.enums.TaskStatus;
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
    private final TaskRepository taskRepository;
    private final ProjectInvitationRepository projectInvitationRepository;

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
        Project project = findActiveProject(projectId);
        ProjectMember myMembership = findMembership(projectId, userId);

        List<ProjectMember> members = projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId);
        long pendingInvitationCount = projectInvitationRepository.countByProjectIdAndStatusAndExpiresAtAfter(
                projectId,
                InvitationStatus.PENDING,
                LocalDateTime.now()
        );
        long todoTaskCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.TODO);
        long inProgressTaskCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(
                projectId,
                TaskStatus.IN_PROGRESS
        );
        long doneTaskCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.DONE);

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
                pendingInvitationCount,
                new ProjectDetailResponse.TaskSummaryResponse(todoTaskCount, inProgressTaskCount, doneTaskCount),
                memberResponses
        );
    }

    @Transactional
    public ProjectSummaryResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        Project project = findActiveProject(projectId);
        ProjectMember membership = findMembership(projectId, userId);
        ensureOwner(membership);

        project.update(request.name(), request.description());

        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                membership.getRole()
        );
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        Project project = findActiveProject(projectId);
        ProjectMember membership = findMembership(projectId, userId);
        ensureOwner(membership);

        project.softDelete();
    }

    public List<ProjectMemberResponse> getProjectMembers(Long userId, Long projectId) {
        findActiveProject(projectId);
        findMembership(projectId, userId);

        return projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(this::toProjectMemberResponse)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse changeMemberRole(
            Long userId,
            Long projectId,
            Long memberId,
            ChangeMemberRoleRequest request
    ) {
        findActiveProject(projectId);
        ProjectMember myMembership = findMembership(projectId, userId);
        ensureOwner(myMembership);

        ProjectMember targetMember = findProjectMember(projectId, memberId);
        validateLastOwnerConstraint(projectId, targetMember, request.role());
        targetMember.changeRole(request.role(), LocalDateTime.now());

        return toProjectMemberResponse(targetMember);
    }

    @Transactional
    public void removeMember(Long userId, Long projectId, Long memberId) {
        findActiveProject(projectId);
        ProjectMember myMembership = findMembership(projectId, userId);
        ensureOwner(myMembership);

        ProjectMember targetMember = findProjectMember(projectId, memberId);
        validateLastOwnerConstraint(projectId, targetMember, null);
        projectMemberRepository.delete(targetMember);
    }

    private ProjectListItemResponse toProjectListItem(ProjectMember projectMember) {
        Project project = projectMember.getProject();
        long memberCount = projectMemberRepository.countByProjectId(project.getId());
        long taskCount = taskRepository.countByProjectIdAndDeletedAtIsNull(project.getId());
        long doneTaskCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(project.getId(), TaskStatus.DONE);
        int progressRate = calculateProgressRate(taskCount, doneTaskCount);

        return new ProjectListItemResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                projectMember.getRole(),
                memberCount,
                taskCount,
                doneTaskCount,
                progressRate,
                project.getUpdatedAt()
        );
    }

    private int calculateProgressRate(long taskCount, long doneTaskCount) {
        if (taskCount <= 0L) {
            return 0;
        }
        return (int) ((doneTaskCount * 100L) / taskCount);
    }

    private ProjectMemberResponse toProjectMemberResponse(ProjectMember projectMember) {
        return new ProjectMemberResponse(
                projectMember.getId(),
                projectMember.getUser().getId(),
                projectMember.getUser().getEmail(),
                projectMember.getUser().getNickname(),
                projectMember.getRole(),
                projectMember.getJoinedAt()
        );
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    private ProjectMember findProjectMember(Long projectId, Long memberId) {
        return projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateLastOwnerConstraint(Long projectId, ProjectMember targetMember, ProjectRole requestedRole) {
        if (targetMember.getRole() != ProjectRole.OWNER) {
            return;
        }

        if (requestedRole == ProjectRole.OWNER) {
            return;
        }

        long ownerCount = projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.OWNER);
        if (ownerCount <= 1) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_LAST_OWNER);
        }
    }

    private void ensureOwner(ProjectMember membership) {
        if (membership.getRole() != ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.ONLY_OWNER_ALLOWED);
        }
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

