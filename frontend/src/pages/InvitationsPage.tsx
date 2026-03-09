import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Mail,
  Check,
  X,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Clock,
  Shield,
  FolderKanban,
} from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  InvitationAction,
  InvitationListItem,
  InvitationListResponse,
  InvitationStatus,
  WorkspaceInvitationAction,
  WorkspaceInvitationListItem,
  WorkspaceInvitationListResponse,
} from '@/types';

type InvitationKind = 'project' | 'workspace';

type InvitationViewItem = {
  kind: InvitationKind;
  invitationId: number;
  targetId: number;
  targetName: string;
  inviterUserId: number;
  inviterNickname: string;
  role: 'OWNER' | 'MEMBER';
  status: InvitationStatus;
  expiresAt: string;
  createdAt: string;
};

const statusVariant: Record<InvitationStatus, 'primary' | 'success' | 'danger' | 'muted' | 'warning'> = {
  PENDING: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  CANCELED: 'muted',
  EXPIRED: 'muted',
};

function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}

function formatDate(dateStr: string): string {
  const [y, m, d] = dateStr.substring(0, 10).split('-');
  return `${y}.${m}.${d}`;
}

function toProjectInvitation(invitation: InvitationListItem): InvitationViewItem {
  return {
    kind: 'project',
    invitationId: invitation.invitationId,
    targetId: invitation.projectId,
    targetName: invitation.projectName,
    inviterUserId: invitation.inviterUserId,
    inviterNickname: invitation.inviterNickname,
    role: invitation.role,
    status: invitation.status,
    expiresAt: invitation.expiresAt,
    createdAt: invitation.createdAt,
  };
}

function toWorkspaceInvitation(invitation: WorkspaceInvitationListItem): InvitationViewItem {
  return {
    kind: 'workspace',
    invitationId: invitation.invitationId,
    targetId: invitation.workspaceId,
    targetName: invitation.workspaceName,
    inviterUserId: invitation.inviterUserId,
    inviterNickname: invitation.inviterNickname,
    role: invitation.role,
    status: invitation.status,
    expiresAt: invitation.expiresAt,
    createdAt: invitation.createdAt,
  };
}

export default function InvitationsPage() {
  const navigate = useNavigate();
  const [activeKind, setActiveKind] = useState<InvitationKind>('project');
  const [invitations, setInvitations] = useState<InvitationViewItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const fetchInvitations = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params: Record<string, string | number> = { page, size: 20 };
      if (statusFilter) params.status = statusFilter;

      if (activeKind === 'project') {
        const res = await apiClient.get<ApiResponse<InvitationListResponse>>('/invitations/me', { params });
        setInvitations(res.data.data.content.map(toProjectInvitation));
        setTotalPages(res.data.data.totalPages);
      } else {
        const res = await apiClient.get<ApiResponse<WorkspaceInvitationListResponse>>('/workspace-invitations/me', {
          params,
        });
        setInvitations(res.data.data.content.map(toWorkspaceInvitation));
        setTotalPages(res.data.data.totalPages);
      }
    } catch (err) {
      setError(activeKind === 'project'
        ? '프로젝트 초대 목록을 불러오는 데 실패했습니다.'
        : '워크스페이스 초대 목록을 불러오는 데 실패했습니다.');
      console.error('Failed to fetch invitations:', err);
    } finally {
      setLoading(false);
    }
  }, [activeKind, page, statusFilter]);

  useEffect(() => {
    void fetchInvitations();
  }, [fetchInvitations]);

  useEffect(() => {
    setPage(0);
    setInvitations([]);
    setTotalPages(0);
    setError(null);
  }, [activeKind]);

  const handleAccept = async (invitation: InvitationViewItem) => {
    setActionLoading(invitation.invitationId);
    try {
      setError(null);
      const path = invitation.kind === 'project'
        ? `/invitations/${invitation.invitationId}/accept`
        : `/workspace-invitations/${invitation.invitationId}/accept`;
      const res = invitation.kind === 'project'
        ? await apiClient.post<ApiResponse<InvitationAction>>(path)
        : await apiClient.post<ApiResponse<WorkspaceInvitationAction>>(path);
      const newStatus = res.data.data.status;
      setInvitations((prev) =>
        prev
          .map((item) => item.invitationId === invitation.invitationId ? { ...item, status: newStatus } : item)
          .filter((item) => !statusFilter || item.status === statusFilter)
      );
    } catch (err) {
      setError(invitation.kind === 'project' ? '초대 수락에 실패했습니다.' : '워크스페이스 초대 수락에 실패했습니다.');
      console.error('Failed to accept:', err);
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (invitation: InvitationViewItem) => {
    setActionLoading(invitation.invitationId);
    try {
      setError(null);
      const path = invitation.kind === 'project'
        ? `/invitations/${invitation.invitationId}/reject`
        : `/workspace-invitations/${invitation.invitationId}/reject`;
      const res = invitation.kind === 'project'
        ? await apiClient.post<ApiResponse<InvitationAction>>(path)
        : await apiClient.post<ApiResponse<WorkspaceInvitationAction>>(path);
      const newStatus = res.data.data.status;
      setInvitations((prev) =>
        prev
          .map((item) => item.invitationId === invitation.invitationId ? { ...item, status: newStatus } : item)
          .filter((item) => !statusFilter || item.status === statusFilter)
      );
    } catch (err) {
      setError(invitation.kind === 'project' ? '초대 거절에 실패했습니다.' : '워크스페이스 초대 거절에 실패했습니다.');
      console.error('Failed to reject:', err);
    } finally {
      setActionLoading(null);
    }
  };

  const navigateToTarget = (invitation: InvitationViewItem) => {
    if (invitation.status !== 'ACCEPTED') {
      return;
    }

    if (invitation.kind === 'project') {
      navigate(`/projects/${invitation.targetId}/board`);
      return;
    }

    navigate(`/workspaces/${invitation.targetId}`);
  };

  return (
    <div>
      <PageHeader
        title="받은 초대"
        description={activeKind === 'project'
          ? '프로젝트 초대 목록을 확인하고 수락 또는 거절하세요.'
          : '워크스페이스 초대 목록을 확인하고 수락 또는 거절하세요.'}
      />

      <div className="mt-[var(--spacing-base)] flex border-b border-[var(--color-border)]">
        <button
          type="button"
          onClick={() => setActiveKind('project')}
          className={`border-b-2 bg-transparent px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--text-sm)] ${
            activeKind === 'project'
              ? 'border-[var(--color-primary)] font-semibold text-[var(--color-text-primary)]'
              : 'border-transparent text-[var(--color-text-secondary)] hover:border-[var(--color-border)] hover:text-[var(--color-text-primary)]'
          }`}
        >
          Project Invitations
        </button>
        <button
          type="button"
          onClick={() => setActiveKind('workspace')}
          className={`border-b-2 bg-transparent px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--text-sm)] ${
            activeKind === 'workspace'
              ? 'border-[var(--color-primary)] font-semibold text-[var(--color-text-primary)]'
              : 'border-transparent text-[var(--color-text-secondary)] hover:border-[var(--color-border)] hover:text-[var(--color-text-primary)]'
          }`}
        >
          Workspace Invitations
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      <div className="flex items-center gap-[var(--spacing-md)] py-[var(--spacing-md)]">
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="">Status: All</option>
          <option value="PENDING">PENDING</option>
          <option value="ACCEPTED">ACCEPTED</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </div>

      {loading && (
        <div className="space-y-3 mt-[var(--spacing-sm)]">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-20 bg-[var(--color-surface-muted)] rounded-[var(--radius-sm)] animate-pulse" />
          ))}
        </div>
      )}

      {!loading && invitations.length === 0 && (
        <div className="text-center py-[var(--spacing-xl)] text-[var(--color-text-muted)] text-[var(--text-sm)]">
          {activeKind === 'project'
            ? <Mail size={32} className="mx-auto mb-[var(--spacing-sm)] opacity-40" />
            : <FolderKanban size={32} className="mx-auto mb-[var(--spacing-sm)] opacity-40" />}
          {statusFilter
            ? '해당 상태의 초대가 없습니다.'
            : activeKind === 'project'
              ? '받은 프로젝트 초대가 없습니다.'
              : '받은 워크스페이스 초대가 없습니다.'}
        </div>
      )}

      {!loading && invitations.length > 0 && (
        <div className="space-y-[var(--spacing-sm)] mt-[var(--spacing-sm)]">
          {invitations.map((invitation) => (
            <div
              key={`${invitation.kind}-${invitation.invitationId}`}
              className="
                border border-[var(--color-border)] rounded-[var(--radius-md)]
                bg-[var(--color-surface)] p-[var(--spacing-base)]
                flex items-center gap-[var(--spacing-base)]
              "
            >
              <div className="
                w-[40px] h-[40px] rounded-[var(--radius-sm)]
                bg-[var(--color-primary)] text-white
                flex items-center justify-center font-bold text-[var(--text-sm)] shrink-0
              ">
                {invitation.targetName.charAt(0).toUpperCase()}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-[var(--spacing-sm)] mb-[var(--spacing-xs)]">
                  <span
                    className="text-[var(--text-sm)] font-semibold text-[var(--color-primary)] cursor-pointer hover:underline"
                    onClick={() => navigateToTarget(invitation)}
                  >
                    {invitation.targetName}
                  </span>
                  <Badge variant={statusVariant[invitation.status]} size="sm">{invitation.status}</Badge>
                  <Badge variant="muted" size="sm">
                    <Shield size={10} className="inline mr-[2px]" />
                    {invitation.role}
                  </Badge>
                </div>
                <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] flex items-center gap-[var(--spacing-md)]">
                  <span>초대자: {invitation.inviterNickname}</span>
                  <span className="flex items-center gap-[2px]">
                    <Clock size={11} />
                    {formatTimeAgo(invitation.createdAt)}
                  </span>
                  <span>만료: {formatDate(invitation.expiresAt)}</span>
                </div>
              </div>

              {invitation.status === 'PENDING' && (
                <div className="flex items-center gap-[var(--spacing-sm)] shrink-0">
                  <button
                    onClick={() => void handleAccept(invitation)}
                    disabled={actionLoading === invitation.invitationId}
                    className="
                      flex items-center gap-1 h-[28px] px-[var(--spacing-sm)]
                      bg-[var(--color-success)] text-white rounded-[var(--radius-sm)]
                      text-[var(--text-xs)] font-medium border-none cursor-pointer
                      hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed
                    "
                  >
                    <Check size={12} />
                    수락
                  </button>
                  <button
                    onClick={() => void handleReject(invitation)}
                    disabled={actionLoading === invitation.invitationId}
                    className="
                      flex items-center gap-1 h-[28px] px-[var(--spacing-sm)]
                      border border-[var(--color-danger)] text-[var(--color-danger)]
                      rounded-[var(--radius-sm)] bg-transparent
                      text-[var(--text-xs)] font-medium cursor-pointer
                      hover:bg-[var(--color-accent-red)] disabled:opacity-50 disabled:cursor-not-allowed
                    "
                  >
                    <X size={12} />
                    거절
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between mt-[var(--spacing-base)] text-[var(--text-sm)]">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="
              flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
              border border-[var(--color-border)] rounded-[var(--radius-sm)]
              bg-[var(--color-surface)] text-[var(--color-text-secondary)]
              cursor-pointer hover:bg-[var(--color-surface-muted)]
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            <ChevronLeft size={14} />
            Prev
          </button>
          <span className="text-[var(--color-text-muted)]">Page {page + 1} / {totalPages}</span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="
              flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
              border border-[var(--color-border)] rounded-[var(--radius-sm)]
              bg-[var(--color-surface)] text-[var(--color-text-secondary)]
              cursor-pointer hover:bg-[var(--color-surface-muted)]
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            Next
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}
