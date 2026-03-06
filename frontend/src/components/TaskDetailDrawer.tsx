import { useState, useEffect } from 'react';
import { X, Edit, Trash2, MessageSquare, Paperclip, Clock } from 'lucide-react';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import type { ApiResponse, TaskDetail, Comment, Attachment, TaskStatus as TStatus } from '@/types';

interface TaskDetailDrawerProps {
  taskId: number;
  onClose: () => void;
  onStatusChange?: (taskId: number, newStatus: string) => void;
}

const statusOptions: { value: TStatus; label: string }[] = [
  { value: 'TODO', label: 'TODO' },
  { value: 'IN_PROGRESS', label: 'IN PROGRESS' },
  { value: 'DONE', label: 'DONE' },
];


const priorityVariant: Record<string, 'danger' | 'warning' | 'primary' | 'muted'> = {
  URGENT: 'danger',
  HIGH: 'warning',
  MEDIUM: 'primary',
  LOW: 'muted',
};

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}

export default function TaskDetailDrawer({ taskId, onClose, onStatusChange }: TaskDetailDrawerProps) {
  const [task, setTask] = useState<TaskDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true);
        const [taskRes, commentsRes, attachmentsRes] = await Promise.all([
          apiClient.get<ApiResponse<TaskDetail>>(`/tasks/${taskId}`),
          apiClient.get<ApiResponse<Comment[]>>(`/tasks/${taskId}/comments`),
          apiClient.get<ApiResponse<Attachment[]>>(`/tasks/${taskId}/attachments`),
        ]);
        setTask(taskRes.data.data);
        setComments(commentsRes.data.data);
        setAttachments(attachmentsRes.data.data);
      } catch {
        // Error handling
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [taskId]);

  const handleStatusChange = async (newStatus: string) => {
    if (!task) return;
    try {
      await apiClient.patch(`/tasks/${taskId}/move`, { status: newStatus });
      setTask({ ...task, status: newStatus as TStatus });
      onStatusChange?.(taskId, newStatus);
    } catch {
      // Error handling
    }
  };

  const handleCommentSubmit = async () => {
    if (!newComment.trim()) return;
    setSubmitting(true);
    try {
      const res = await apiClient.post<ApiResponse<Comment>>(`/tasks/${taskId}/comments`, {
        content: newComment,
      });
      setComments([...comments, res.data.data]);
      setNewComment('');
    } catch {
      // Error handling
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/20 z-50"
        onClick={onClose}
      />

      {/* Drawer */}
      <div className="
        fixed top-0 right-0 h-full w-[680px] max-w-full
        bg-[var(--color-surface)] border-l border-[var(--color-border)]
        z-50 overflow-y-auto
        shadow-[-4px_0_12px_rgba(0,0,0,0.05)]
      ">
        {/* Close button */}
        <div className="sticky top-0 bg-[var(--color-surface)] border-b border-[var(--color-border)] px-[var(--spacing-lg)] py-[var(--spacing-md)] flex items-center justify-between z-10">
          <div className="flex items-center gap-[var(--spacing-sm)]">
            <span className="text-[var(--text-xs)] text-[var(--color-text-muted)] font-mono">
              TASK-{taskId}
            </span>
          </div>
          <div className="flex items-center gap-[var(--spacing-sm)]">
            <button className="
              p-[var(--spacing-xs)] rounded-[var(--radius-sm)]
              text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]
              bg-transparent border-none cursor-pointer
            ">
              <Edit size={14} />
            </button>
            <button className="
              p-[var(--spacing-xs)] rounded-[var(--radius-sm)]
              text-[var(--color-danger)] hover:bg-[var(--color-accent-red)]
              bg-transparent border-none cursor-pointer
            ">
              <Trash2 size={14} />
            </button>
            <button
              onClick={onClose}
              className="
                p-[var(--spacing-xs)] rounded-[var(--radius-sm)]
                text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]
                bg-transparent border-none cursor-pointer
              "
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {loading ? (
          <div className="p-[var(--spacing-lg)] animate-pulse space-y-4">
            <div className="h-6 bg-[var(--color-surface-muted)] rounded w-3/4" />
            <div className="h-4 bg-[var(--color-surface-muted)] rounded w-1/2" />
            <div className="h-32 bg-[var(--color-surface-muted)] rounded" />
          </div>
        ) : task ? (
          <div className="flex">
            {/* Main content (left) */}
            <div className="flex-1 p-[var(--spacing-lg)] border-r border-[var(--color-border)] min-w-0">
              {/* Title */}
              <h2 className="text-[var(--text-lg)] font-bold text-[var(--color-text-primary)] m-0 mb-[var(--spacing-xs)]">
                {task.title}
              </h2>
              <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] mb-[var(--spacing-lg)]">
                opened by {task.creator.name} · {formatTimeAgo(task.createdAt)} · updated {formatTimeAgo(task.updatedAt)}
              </div>

              {/* Description */}
              <div className="mb-[var(--spacing-lg)]">
                <h3 className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)] mb-[var(--spacing-sm)] m-0">
                  Description
                </h3>
                <div className="
                  text-[var(--text-sm)] text-[var(--color-text-secondary)] leading-relaxed
                  p-[var(--spacing-base)] bg-[var(--color-surface-muted)]
                  rounded-[var(--radius-sm)] border border-[var(--color-border-muted)]
                ">
                  {task.description || '설명이 없습니다.'}
                </div>
              </div>

              {/* Activity / Comments */}
              <div>
                <h3 className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)] mb-[var(--spacing-md)] m-0 flex items-center gap-[var(--spacing-xs)]">
                  <MessageSquare size={14} />
                  Activity ({comments.length})
                </h3>

                {comments.length === 0 && (
                  <div className="text-[var(--text-sm)] text-[var(--color-text-muted)] mb-[var(--spacing-base)]">
                    아직 댓글이 없습니다.
                  </div>
                )}

                {/* Comment list */}
                <div className="space-y-[var(--spacing-md)] mb-[var(--spacing-lg)]">
                  {comments.map((comment) => (
                    <div key={comment.id} className="flex gap-[var(--spacing-sm)]">
                      <div className="
                        w-[24px] h-[24px] rounded-full bg-[var(--color-primary)]
                        text-white text-[10px] font-semibold
                        flex items-center justify-center shrink-0 mt-[2px]
                      ">
                        {comment.author.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-[var(--spacing-sm)] mb-[var(--spacing-xs)]">
                          <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                            {comment.author.name}
                          </span>
                          <span className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                            {formatTimeAgo(comment.createdAt)}
                          </span>
                        </div>
                        <div className="
                          text-[var(--text-sm)] text-[var(--color-text-secondary)]
                          p-[var(--spacing-sm)] bg-[var(--color-surface-muted)]
                          rounded-[var(--radius-sm)] border border-[var(--color-border-muted)]
                        ">
                          {comment.content}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Comment input */}
                <div className="border-t border-[var(--color-border)] pt-[var(--spacing-md)]">
                  <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder="댓글을 작성하세요..."
                    rows={3}
                    className="
                      w-full p-[var(--spacing-sm)]
                      border border-[var(--color-border)] rounded-[var(--radius-sm)]
                      bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                      placeholder:text-[var(--color-text-muted)] resize-vertical
                      focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                    "
                  />
                  <div className="flex justify-end mt-[var(--spacing-sm)]">
                    <button
                      onClick={handleCommentSubmit}
                      disabled={!newComment.trim() || submitting}
                      className="
                        h-[32px] px-[var(--spacing-md)]
                        bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
                        text-[var(--text-sm)] font-medium border-none cursor-pointer
                        hover:bg-[var(--color-primary-hover)]
                        disabled:opacity-50 disabled:cursor-not-allowed
                      "
                    >
                      {submitting ? '전송 중...' : 'Comment'}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Right sidebar meta panel */}
            <div className="w-[220px] p-[var(--spacing-base)] shrink-0 space-y-[var(--spacing-base)]">
              {/* Status */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)]">
                  Status
                </div>
                <select
                  value={task.status}
                  onChange={(e) => handleStatusChange(e.target.value)}
                  className="
                    w-full h-[28px] px-[var(--spacing-xs)]
                    border border-[var(--color-border)] rounded-[var(--radius-sm)]
                    bg-[var(--color-surface)] text-[var(--text-sm)]
                    focus:outline-none focus:border-[var(--color-primary)]
                  "
                >
                  {statusOptions.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>

              {/* Assignee */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)]">
                  Assignee
                </div>
                {task.assignee ? (
                  <div className="flex items-center gap-[var(--spacing-xs)]">
                    <div className="
                      w-[20px] h-[20px] rounded-full bg-[var(--color-primary)]
                      text-white text-[10px] font-semibold
                      flex items-center justify-center
                    ">
                      {task.assignee.name.charAt(0).toUpperCase()}
                    </div>
                    <span className="text-[var(--text-sm)] text-[var(--color-text-primary)]">
                      {task.assignee.name}
                    </span>
                  </div>
                ) : (
                  <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">미배정</span>
                )}
              </div>

              {/* Priority */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)]">
                  Priority
                </div>
                <Badge variant={priorityVariant[task.priority] || 'muted'}>
                  {task.priority}
                </Badge>
              </div>

              {/* Due date */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)]">
                  Due Date
                </div>
                <span className="text-[var(--text-sm)] text-[var(--color-text-primary)]">
                  {task.dueDate ? formatDate(task.dueDate) : '-'}
                </span>
              </div>

              {/* Creator */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)]">
                  Creator
                </div>
                <span className="text-[var(--text-sm)] text-[var(--color-text-primary)]">
                  {task.creator.name}
                </span>
              </div>

              {/* Labels */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)]">
                  Labels
                </div>
                {task.labels.length > 0 ? (
                  <div className="flex flex-wrap gap-1">
                    {task.labels.map((label) => (
                      <span
                        key={label.id}
                        className="
                          inline-block px-[5px] py-[1px] text-[11px] font-medium
                          rounded-[var(--radius-sm)] border
                        "
                        style={{
                          backgroundColor: `${label.color}20`,
                          borderColor: `${label.color}40`,
                          color: label.color,
                        }}
                      >
                        {label.name}
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">없음</span>
                )}
              </div>

              {/* Attachments */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-xs)]">
                  <Paperclip size={11} />
                  Attachments ({attachments.length})
                </div>
                {attachments.length > 0 ? (
                  <div className="space-y-1">
                    {attachments.map((file) => (
                      <a
                        key={file.id}
                        href={file.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="
                          block text-[var(--text-xs)] text-[var(--color-primary)] truncate
                          hover:underline
                        "
                      >
                        {file.fileName}
                      </a>
                    ))}
                  </div>
                ) : (
                  <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">없음</span>
                )}
              </div>

              {/* Recent Status History placeholder */}
              <div>
                <div className="text-[var(--text-xs)] font-semibold text-[var(--color-text-muted)] uppercase mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-xs)]">
                  <Clock size={11} />
                  Status History
                </div>
                <span className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                  상태 이력이 여기 표시됩니다
                </span>
              </div>
            </div>
          </div>
        ) : (
          <div className="p-[var(--spacing-lg)] text-[var(--color-text-muted)]">
            태스크를 불러올 수 없습니다.
          </div>
        )}
      </div>
    </>
  );
}
