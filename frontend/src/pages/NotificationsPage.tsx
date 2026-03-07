import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell, Check, ChevronLeft, ChevronRight, AlertCircle,
  UserPlus, UserCheck, ClipboardList, MessageSquare, AtSign,
} from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import apiClient from '@/api/client';
import type {
  ApiResponse, NotificationItem, NotificationListResponse,
  NotificationUnreadCount, NotificationType, NotificationReferenceType,
} from '@/types';

const typeIcons: Record<NotificationType, typeof Bell> = {
  PROJECT_INVITED: UserPlus,
  INVITATION_ACCEPTED: UserCheck,
  TASK_ASSIGNED: ClipboardList,
  COMMENT_CREATED: MessageSquare,
  COMMENT_MENTIONED: AtSign,
};

const typeColors: Record<NotificationType, string> = {
  PROJECT_INVITED: 'var(--color-primary)',
  INVITATION_ACCEPTED: 'var(--color-success)',
  TASK_ASSIGNED: 'var(--color-warning)',
  COMMENT_CREATED: 'var(--color-text-muted)',
  COMMENT_MENTIONED: 'var(--color-danger)',
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

function getNavigationPath(refType: NotificationReferenceType, refId: number): string | null {
  switch (refType) {
    case 'PROJECT': return `/projects/${refId}/board`;
    case 'TASK': return null; // task drawer needs project context
    case 'INVITATION': return '/invitations';
    case 'COMMENT': return null;
    default: return null;
  }
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchNotifications = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [listRes, countRes] = await Promise.all([
        apiClient.get<ApiResponse<NotificationListResponse>>('/notifications', {
          params: { page, size: 20, unreadOnly },
        }),
        apiClient.get<ApiResponse<NotificationUnreadCount>>('/notifications/unread-count'),
      ]);
      setNotifications(listRes.data.data.content);
      setTotalPages(listRes.data.data.totalPages);
      setUnreadCount(countRes.data.data.unreadCount);
    } catch (err) {
      setError('알림을 불러오는 데 실패했습니다.');
      console.error('Failed to fetch notifications:', err);
    } finally {
      setLoading(false);
    }
  }, [page, unreadOnly]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const handleRead = async (notificationId: number) => {
    try {
      await apiClient.patch(`/notifications/${notificationId}/read`);
      setNotifications((prev) =>
        unreadOnly
          ? prev.filter((n) => n.notificationId !== notificationId)
          : prev.map((n) => n.notificationId === notificationId ? { ...n, isRead: true } : n)
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch (err) {
      console.error('Failed to read notification:', err);
    }
  };

  const handleReadAll = async () => {
    try {
      setError(null);
      await apiClient.post('/notifications/read-all');
      setUnreadCount(0);
      // When unreadOnly filter is active, refetch to remove read items from the list.
      // Otherwise just update local state for immediate UI feedback.
      if (unreadOnly) {
        fetchNotifications();
      } else {
        setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      }
    } catch (err) {
      setError('모두 읽기에 실패했습니다.');
      console.error('Failed to read all:', err);
    }
  };

  const handleNotificationClick = (n: NotificationItem) => {
    // Mark as read
    if (!n.isRead) handleRead(n.notificationId);
    // Navigate if possible
    const path = getNavigationPath(n.referenceType, n.referenceId);
    if (path) navigate(path);
  };

  return (
    <div>
      <PageHeader title="알림" description="프로젝트 활동 관련 알림을 확인하세요.">
        <div className="flex items-center gap-[var(--spacing-sm)]">
          {unreadCount > 0 && (
            <span className="
              px-[6px] py-[1px] text-[11px] font-bold
              bg-[var(--color-danger)] text-white rounded-full
            ">
              {unreadCount}
            </span>
          )}
          <label className="flex items-center gap-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)] cursor-pointer">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(e) => { setUnreadOnly(e.target.checked); setPage(0); }}
              className="accent-[var(--color-primary)]"
            />
            읽지 않은 알림만
          </label>
          <button
            onClick={handleReadAll}
            disabled={unreadCount === 0}
            className="
              flex items-center gap-1 h-[28px] px-[var(--spacing-sm)]
              border border-[var(--color-border)] rounded-[var(--radius-sm)]
              bg-[var(--color-surface)] text-[var(--text-xs)] text-[var(--color-text-secondary)]
              cursor-pointer hover:bg-[var(--color-surface-muted)]
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            <Check size={12} />
            모두 읽음
          </button>
        </div>
      </PageHeader>

      {/* Error */}
      {error && (
        <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="space-y-2 mt-[var(--spacing-sm)]">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-16 bg-[var(--color-surface-muted)] rounded-[var(--radius-sm)] animate-pulse" />
          ))}
        </div>
      )}

      {/* Empty */}
      {!loading && notifications.length === 0 && (
        <div className="text-center py-[var(--spacing-xl)] text-[var(--color-text-muted)] text-[var(--text-sm)]">
          <Bell size={32} className="mx-auto mb-[var(--spacing-sm)] opacity-40" />
          {unreadOnly ? '읽지 않은 알림이 없습니다.' : '알림이 없습니다.'}
        </div>
      )}

      {/* Notification list */}
      {!loading && notifications.length > 0 && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden mt-[var(--spacing-sm)]">
          {notifications.map((n, idx) => {
            const Icon = typeIcons[n.type] || Bell;
            const iconColor = typeColors[n.type] || 'var(--color-text-muted)';
            return (
              <div
                key={n.notificationId}
                onClick={() => handleNotificationClick(n)}
                className={`
                  flex items-start gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-md)]
                  cursor-pointer hover:bg-[var(--color-surface-muted)] transition-colors
                  ${!n.isRead ? 'bg-[var(--color-accent-blue)]' : ''}
                  ${idx < notifications.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                `}
              >
                {/* Icon */}
                <div
                  className="w-[32px] h-[32px] rounded-full flex items-center justify-center shrink-0 mt-[2px]"
                  style={{ backgroundColor: `color-mix(in srgb, ${iconColor} 15%, transparent)` }}
                >
                  <Icon size={16} style={{ color: iconColor }} />
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-[var(--spacing-sm)]">
                    <span className={`text-[var(--text-sm)] ${!n.isRead ? 'font-semibold text-[var(--color-text-primary)]' : 'text-[var(--color-text-secondary)]'}`}>
                      {n.title}
                    </span>
                    {!n.isRead && (
                      <span className="w-[6px] h-[6px] rounded-full bg-[var(--color-primary)] shrink-0" />
                    )}
                  </div>
                  {n.content && (
                    <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] mt-[2px] truncate">
                      {n.content}
                    </div>
                  )}
                  <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] mt-[2px]">
                    {formatTimeAgo(n.createdAt)}
                  </div>
                </div>
              </div>
            );
          })}
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
