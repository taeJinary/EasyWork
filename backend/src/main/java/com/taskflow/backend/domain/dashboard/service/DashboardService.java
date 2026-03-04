package com.taskflow.backend.domain.dashboard.service;

import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectStatsResponse;
import com.taskflow.backend.domain.dashboard.dto.response.DashboardProjectsResponse;
import com.taskflow.backend.domain.invitation.repository.ProjectInvitationRepository;
import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.project.entity.ProjectMember;
import com.taskflow.backend.domain.project.repository.ProjectMemberRepository;
import com.taskflow.backend.domain.project.repository.ProjectRepository;
import com.taskflow.backend.domain.task.repository.TaskRepository;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.global.common.enums.InvitationStatus;
import com.taskflow.backend.global.common.enums.TaskStatus;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int DUE_SOON_DAYS = 3;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final TaskRepository taskRepository;

    public DashboardProjectsResponse getDashboardProjects(Long userId) {
        findActiveUser(userId);

        long pendingInvitationCount = projectInvitationRepository.countByInviteeIdAndStatusAndExpiresAtAfter(
                userId,
                InvitationStatus.PENDING,
                LocalDateTime.now()
        );

        List<DashboardProjectsResponse.MyProjectResponse> myProjects = projectMemberRepository
                .findAllActiveByUserIdOrderByProjectUpdatedAtDesc(userId)
                .stream()
                .map(this::toMyProjectResponse)
                .toList();

        return new DashboardProjectsResponse(pendingInvitationCount, myProjects);
    }

    public DashboardProjectStatsResponse getProjectDashboard(Long userId, Long projectId) {
        findActiveProject(projectId);
        findMembership(projectId, userId);

        long memberCount = projectMemberRepository.countByProjectId(projectId);
        long taskCount = taskRepository.countByProjectIdAndDeletedAtIsNull(projectId);
        long todoCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.TODO);
        long inProgressCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.IN_PROGRESS);
        long doneCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, TaskStatus.DONE);

        LocalDate today = LocalDate.now();
        long overdueCount = taskRepository.countByProjectIdAndDeletedAtIsNullAndDueDateBeforeAndStatusNot(
                projectId,
                today,
                TaskStatus.DONE
        );
        long dueSoonCount = taskRepository.countByProjectIdAndDeletedAtIsNullAndDueDateBetweenAndStatusNot(
                projectId,
                today,
                today.plusDays(DUE_SOON_DAYS),
                TaskStatus.DONE
        );

        return new DashboardProjectStatsResponse(
                projectId,
                memberCount,
                taskCount,
                todoCount,
                inProgressCount,
                doneCount,
                overdueCount,
                dueSoonCount,
                calculateCompletionRate(taskCount, doneCount)
        );
    }

    private DashboardProjectsResponse.MyProjectResponse toMyProjectResponse(ProjectMember membership) {
        Project project = membership.getProject();
        long memberCount = projectMemberRepository.countByProjectId(project.getId());
        long taskCount = taskRepository.countByProjectIdAndDeletedAtIsNull(project.getId());
        long doneTaskCount = taskRepository.countByProjectIdAndStatusAndDeletedAtIsNull(project.getId(), TaskStatus.DONE);

        return new DashboardProjectsResponse.MyProjectResponse(
                project.getId(),
                project.getName(),
                membership.getRole(),
                memberCount,
                taskCount,
                doneTaskCount,
                calculateCompletionRate(taskCount, doneTaskCount),
                project.getUpdatedAt()
        );
    }

    private int calculateCompletionRate(long taskCount, long doneCount) {
        if (taskCount <= 0L) {
            return 0;
        }
        return (int) ((doneCount * 100L) / taskCount);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember findMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));
    }
}
