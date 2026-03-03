package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListItemResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkspaceSummaryResponse createWorkspace(Long userId, CreateWorkspaceRequest request) {
        User owner = findUser(userId);
        Workspace workspace = Workspace.create(
                owner,
                request.name().trim(),
                normalizeDescription(request.description())
        );
        Workspace saved = workspaceRepository.save(workspace);
        workspaceMemberRepository.save(WorkspaceMember.create(
                saved,
                owner,
                WorkspaceRole.OWNER,
                LocalDateTime.now()
        ));
        return new WorkspaceSummaryResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                WorkspaceRole.OWNER
        );
    }

    public WorkspaceListResponse getMyWorkspaces(Long userId, int page, int size) {
        findUser(userId);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizePageSize(size);
        List<WorkspaceMember> memberships = workspaceMemberRepository.findAllByUserIdOrderByWorkspaceUpdatedAtDesc(userId);

        int fromIndex = Math.min(normalizedPage * normalizedSize, memberships.size());
        int toIndex = Math.min(fromIndex + normalizedSize, memberships.size());
        List<WorkspaceListItemResponse> content = memberships.subList(fromIndex, toIndex).stream()
                .map(this::toListItem)
                .toList();

        long totalElements = memberships.size();
        int totalPages = normalizedSize == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        boolean first = normalizedPage == 0;
        boolean last = totalPages == 0 || normalizedPage >= totalPages - 1;

        return new WorkspaceListResponse(
                content,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                first,
                last
        );
    }

    private WorkspaceListItemResponse toListItem(WorkspaceMember membership) {
        Workspace workspace = membership.getWorkspace();
        return new WorkspaceListItemResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                membership.getRole(),
                workspaceMemberRepository.countByWorkspaceId(workspace.getId()),
                workspace.getUpdatedAt()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }
}
