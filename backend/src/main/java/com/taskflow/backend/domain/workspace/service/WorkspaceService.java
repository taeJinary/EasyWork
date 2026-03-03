package com.taskflow.backend.domain.workspace.service;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.domain.user.repository.UserRepository;
import com.taskflow.backend.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListItemResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceListResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceMemberResponse;
import com.taskflow.backend.domain.workspace.dto.response.WorkspaceSummaryResponse;
import com.taskflow.backend.domain.workspace.entity.Workspace;
import com.taskflow.backend.domain.workspace.entity.WorkspaceMember;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberCountProjection;
import com.taskflow.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.taskflow.backend.domain.workspace.repository.WorkspaceRepository;
import com.taskflow.backend.global.common.enums.WorkspaceRole;
import com.taskflow.backend.global.error.BusinessException;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);
        Page<WorkspaceMember> membershipPage =
                workspaceMemberRepository.findByUserIdOrderByWorkspaceUpdatedAtDesc(userId, pageable);
        Map<Long, Long> memberCounts = loadMemberCounts(membershipPage.getContent());

        List<WorkspaceListItemResponse> content = membershipPage.getContent().stream()
                .map(membership -> toListItem(membership, memberCounts))
                .toList();

        long totalElements = membershipPage.getTotalElements();
        int totalPages = membershipPage.getTotalPages();
        boolean first = membershipPage.isFirst();
        boolean last = totalPages == 0 || membershipPage.isLast();

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

    public WorkspaceDetailResponse getWorkspaceDetail(Long userId, Long workspaceId) {
        findUser(userId);
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember myMembership = findWorkspaceMembership(workspaceId, userId);

        return new WorkspaceDetailResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                myMembership.getRole(),
                workspaceMemberRepository.countByWorkspaceId(workspace.getId()),
                workspace.getUpdatedAt()
        );
    }

    public List<WorkspaceMemberResponse> getWorkspaceMembers(Long userId, Long workspaceId) {
        findUser(userId);
        findWorkspace(workspaceId);
        findWorkspaceMembership(workspaceId, userId);

        return workspaceMemberRepository.findAllWithUserByWorkspaceIdOrderByJoinedAtAsc(workspaceId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public WorkspaceSummaryResponse updateWorkspace(Long userId, Long workspaceId, UpdateWorkspaceRequest request) {
        findUser(userId);
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember membership = findWorkspaceMembership(workspaceId, userId);
        ensureOwner(membership);

        workspace.update(request.name(), request.description(), LocalDateTime.now());

        return new WorkspaceSummaryResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                membership.getRole()
        );
    }

    @Transactional
    public void deleteWorkspace(Long userId, Long workspaceId) {
        findUser(userId);
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember membership = findWorkspaceMembership(workspaceId, userId);
        ensureOwner(membership);

        workspaceMemberRepository.deleteAllByWorkspaceId(workspaceId);
        workspaceRepository.delete(workspace);
    }

    private WorkspaceListItemResponse toListItem(WorkspaceMember membership, Map<Long, Long> memberCounts) {
        Workspace workspace = membership.getWorkspace();
        return new WorkspaceListItemResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                membership.getRole(),
                memberCounts.getOrDefault(workspace.getId(), 0L),
                workspace.getUpdatedAt()
        );
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember membership) {
        return new WorkspaceMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getUser().getNickname(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }

    private Map<Long, Long> loadMemberCounts(List<WorkspaceMember> memberships) {
        Set<Long> workspaceIds = memberships.stream()
                .map(membership -> membership.getWorkspace().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (workspaceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workspaceMemberRepository.countMembersByWorkspaceIds(workspaceIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        WorkspaceMemberCountProjection::getWorkspaceId,
                        WorkspaceMemberCountProjection::getMemberCount
                ));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Workspace findWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private WorkspaceMember findWorkspaceMembership(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private void ensureOwner(WorkspaceMember membership) {
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.ONLY_OWNER_ALLOWED);
        }
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
