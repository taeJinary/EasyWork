import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, Check, X, ChevronLeft, ChevronRight, AlertCircle, Clock, Shield } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import type { ApiResponse, InvitationListItem, InvitationListResponse, InvitationAction, InvitationStatus } from '@/types';

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

export default function InvitationsPage() {
  const navigate = useNavigate();
  const [invitations, setInvitations] = useState<InvitationListItem[]>([]);
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
      const res = await apiClient.get<ApiResponse<InvitationListResponse>>('/invitations/me', { params });
      setInvitations(res.data.data.content);
      setTotalPages(res.data.data.totalPages);
    } catch (err) {
      setError('초대 목록을 불러오는 데 실패했습니다.');
      console.error('Failed to fetch invitations:', err);
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    fetchInvitations();
  }, [fetchInvitations]);

  const handleAccept = async (invitationId: number) => {
    setActionLoading(invitationId);
    try {
      setError(null);
      const res = await apiClient.post<ApiResponse<InvitationAction>>(`/invitations/${invitationId}/accept`);
      const newStatus = res.data.data.status;
      setInvitations((prev) =>
        prev.map((inv) => inv.invitationId === invitationId ? { ...inv, status: newStatus } : inv)
      );
    } catch (err) {
      setError('초대 수락에 실패했습니다.');
      console.error('Failed to accept:', err);
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (invitationId: number) => {
    setActionLoading(invitationId);
    try {
      setError(null);
      const res = await apiClient.post<ApiResponse<InvitationAction>>(`/invitations/${invitationId}/reject`);
      const newStatus = res.data.data.status;
      setInvitations((prev) =>
        prev.map((inv) => inv.invitationId === invitationId ? { ...inv, status: newStatus } : inv)
      );
    } catch (err) {
      setError('초대 거절에 실패했습니다.');
      console.error('Failed to reject:', err);
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div>
      <PageHeader title="받은 초대" description="프로젝트 초대 목록을 확인하고 수락 또는 거절하세요." />

      {/* Error */}
      {error && (
        <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      {/* Filter */}
      <FilterBar searchPlaceholder="" searchValue="" onSearchChange={() => {}}>
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
      </FilterBar>

      {/* Loading */}
      {loading && (
        <div className="space-y-3 mt-[var(--spacing-sm)]">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-20 bg-[var(--color-surface-muted)] rounded-[var(--radius-sm)] animate-pulse" />
          ))}
        </div>
      )}

      {/* Empty */}
      {!loading && invitations.length === 0 && (
        <div className="text-center py-[var(--spacing-xl)] text-[var(--color-text-muted)] text-[var(--text-sm)]">
          <Mail size={32} className="mx-auto mb-[var(--spacing-sm)] opacity-40" />
          {statusFilter ? '해당 상태의 초대가 없습니다.' : '받은 초대가 없습니다.'}
        </div>
      )}

      {/* Invitation list */}
      {!loading && invitations.length > 0 && (
        <div className="space-y-[var(--spacing-sm)] mt-[var(--spacing-sm)]">
          {invitations.map((inv) => (
            <div
              key={inv.invitationId}
              className="
                border border-[var(--color-border)] rounded-[var(--radius-md)]
                bg-[var(--color-surface)] p-[var(--spacing-base)]
                flex items-center gap-[var(--spacing-base)]
              "
            >
              {/* Project icon */}
              <div className="
                w-[40px] h-[40px] rounded-[var(--radius-sm)]
                bg-[var(--color-primary)] text-white
                flex items-center justify-center font-bold text-[var(--text-sm)] shrink-0
              ">
                {inv.projectName.charAt(0).toUpperCase()}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-[var(--spacing-sm)] mb-[var(--spacing-xs)]">
                  <span
                    className="text-[var(--text-sm)] font-semibold text-[var(--color-primary)] cursor-pointer hover:underline"
                    onClick={() => inv.status === 'ACCEPTED' && navigate(`/projects/${inv.projectId}/board`)}
                  >
                    {inv.projectName}
                  </span>
                  <Badge variant={statusVariant[inv.status]} size="sm">{inv.status}</Badge>
                  <Badge variant="muted" size="sm">
                    <Shield size={10} className="inline mr-[2px]" />
                    {inv.role}
                  </Badge>
                </div>
                <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] flex items-center gap-[var(--spacing-md)]">
                  <span>초대자: {inv.inviterNickname}</span>
                  <span className="flex items-center gap-[2px]">
                    <Clock size={11} />
                    {formatTimeAgo(inv.createdAt)}
                  </span>
                  <span>만료: {formatDate(inv.expiresAt)}</span>
                </div>
              </div>

              {/* Actions — only for PENDING */}
              {inv.status === 'PENDING' && (
                <div className="flex items-center gap-[var(--spacing-sm)] shrink-0">
                  <button
                    onClick={() => handleAccept(inv.invitationId)}
                    disabled={actionLoading === inv.invitationId}
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
                    onClick={() => handleReject(inv.invitationId)}
                    disabled={actionLoading === inv.invitationId}
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

      {/* Pagination */}
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
