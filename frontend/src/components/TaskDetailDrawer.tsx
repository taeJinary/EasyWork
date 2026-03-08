import { useEffect, useState } from 'react';
import { AlertCircle, Clock, Edit, MessageSquare, Paperclip, Trash2, X } from 'lucide-react';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import { useAuthStore } from '@/stores/authStore';
import type {
  ApiResponse,
  Attachment,
  Comment,
  CommentListResponse,
  ProjectLabelResponse,
  ProjectMember,
  TaskDetail,
  TaskMoveResponse,
  TaskPriority,
  TaskStatus as TStatus,
} from '@/types';

interface TaskDetailDrawerProps {
  taskId: number;
  onClose: () => void;
  onStatusChange?: (taskId: number, newStatus: string) => void;
  onTaskUpdated?: () => void;
  onTaskDeleted?: (taskId: number) => void;
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

const priorityOptions: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-');
  return `${year}.${month}.${day}`;
}

function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export default function TaskDetailDrawer({
  taskId,
  onClose,
  onStatusChange,
  onTaskUpdated,
  onTaskDeleted,
}: TaskDetailDrawerProps) {
  const { user } = useAuthStore();
  const [task, setTask] = useState<TaskDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [projectMembers, setProjectMembers] = useState<ProjectMember[]>([]);
  const [projectLabels, setProjectLabels] = useState<ProjectLabelResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [deletingAttachmentId, setDeletingAttachmentId] = useState<number | null>(null);
  const [savingTask, setSavingTask] = useState(false);
  const [deletingTask, setDeletingTask] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editAssigneeUserId, setEditAssigneeUserId] = useState<string>('');
  const [editPriority, setEditPriority] = useState<TaskPriority>('MEDIUM');
  const [editDueDate, setEditDueDate] = useState('');
  const [editLabelIds, setEditLabelIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true);
        setError(null);
        const [taskResponse, commentsResponse, attachmentsResponse] = await Promise.all([
          apiClient.get<ApiResponse<TaskDetail>>(`/tasks/${taskId}`),
          apiClient.get<ApiResponse<CommentListResponse>>(`/tasks/${taskId}/comments`),
          apiClient.get<ApiResponse<Attachment[]>>(`/tasks/${taskId}/attachments`),
        ]);
        setTask(taskResponse.data.data);
        setComments(commentsResponse.data.data.content);
        setAttachments(attachmentsResponse.data.data);
      } catch (caughtError) {
        setError('Failed to load task.');
        console.error('Failed to fetch task detail:', caughtError);
      } finally {
        setLoading(false);
      }
    }

    void fetchData();
  }, [taskId]);

  useEffect(() => {
    setIsEditing(false);
    setEditTitle('');
    setEditDescription('');
    setEditAssigneeUserId('');
    setEditPriority('MEDIUM');
    setEditDueDate('');
    setEditLabelIds([]);
  }, [taskId]);

  useEffect(() => {
    if (!task?.projectId) {
      setProjectMembers([]);
      setProjectLabels([]);
      return;
    }

    const projectId = task.projectId;
    let cancelled = false;

    setProjectMembers([]);
    setProjectLabels([]);

    async function fetchEditOptions() {
      try {
        const [membersResponse, labelsResponse] = await Promise.all([
          apiClient.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`),
          apiClient.get<ApiResponse<ProjectLabelResponse[]>>(`/projects/${projectId}/labels`),
        ]);

        if (cancelled) {
          return;
        }

        setProjectMembers(membersResponse.data.data);
        setProjectLabels(labelsResponse.data.data);
      } catch (caughtError) {
        if (!cancelled) {
          setProjectMembers([]);
          setProjectLabels([]);
          console.error('Failed to load task edit options:', caughtError);
        }
      }
    }

    void fetchEditOptions();

    return () => {
      cancelled = true;
    };
  }, [task?.projectId]);

  const handleStatusChange = async (newStatus: string) => {
    if (!task) return;

    try {
      setError(null);
      const response = await apiClient.patch<ApiResponse<TaskMoveResponse>>(`/tasks/${taskId}/move`, {
        toStatus: newStatus,
        targetPosition: 0,
        version: task.version,
      });
      const movedTask = response.data.data;
      setTask((current) =>
        current
          ? {
              ...current,
              status: movedTask.status,
              version: movedTask.version,
            }
          : current
      );
      onStatusChange?.(taskId, movedTask.status);
    } catch (caughtError) {
      setError('Failed to update task status.');
      console.error('Failed to move task:', caughtError);
    }
  };

  const openEditModal = () => {
    if (!task) {
      return;
    }

    setEditTitle(task.title);
    setEditDescription(task.description ?? '');
    setEditAssigneeUserId(task.assignee?.userId ? String(task.assignee.userId) : '');
    setEditPriority(task.priority);
    setEditDueDate(task.dueDate ?? '');
    setEditLabelIds(task.labels.map((label) => label.labelId));
    setError(null);
    setIsEditing(true);
  };

  const handleTaskSave = async () => {
    if (!task || !editTitle.trim()) {
      return;
    }

    try {
      setSavingTask(true);
      setError(null);
      const response = await apiClient.patch<ApiResponse<TaskDetail>>(`/tasks/${taskId}`, {
        title: editTitle.trim(),
        description: editDescription.trim(),
        assigneeUserId: editAssigneeUserId ? Number(editAssigneeUserId) : null,
        priority: editPriority,
        dueDate: editDueDate || null,
        labelIds: editLabelIds,
        version: task.version,
      });
      setTask(response.data.data);
      setIsEditing(false);
      onTaskUpdated?.();
    } catch (caughtError) {
      setError('Failed to update task.');
      console.error('Failed to update task:', caughtError);
    } finally {
      setSavingTask(false);
    }
  };

  const toggleEditLabel = (labelId: number) => {
    setEditLabelIds((current) =>
      current.includes(labelId) ? current.filter((id) => id !== labelId) : [...current, labelId]
    );
  };

  const handleTaskDelete = async () => {
    if (!task || !window.confirm('Delete this task?')) {
      return;
    }

    try {
      setDeletingTask(true);
      setError(null);
      await apiClient.delete(`/tasks/${taskId}`);
      onTaskDeleted?.(taskId);
      onClose();
    } catch (caughtError) {
      setError('Failed to delete task.');
      console.error('Failed to delete task:', caughtError);
    } finally {
      setDeletingTask(false);
    }
  };

  const handleCommentSubmit = async () => {
    if (!newComment.trim()) return;

    setSubmitting(true);
    try {
      setError(null);
      const response = await apiClient.post<ApiResponse<Comment>>(`/tasks/${taskId}/comments`, {
        content: newComment,
      });
      setComments((current) => [...current, response.data.data]);
      setNewComment('');
      setTask((current) =>
        current
          ? {
              ...current,
              commentCount: current.commentCount + 1,
            }
          : current
      );
    } catch (caughtError) {
      setError('Failed to submit comment.');
      console.error('Failed to submit comment:', caughtError);
    } finally {
      setSubmitting(false);
    }
  };

  const handleAttachmentDelete = async (attachmentId: number) => {
    try {
      setDeletingAttachmentId(attachmentId);
      setError(null);
      await apiClient.delete(`/attachments/${attachmentId}`);
      setAttachments((current) => current.filter((attachment) => attachment.attachmentId !== attachmentId));
    } catch (caughtError) {
      setError('Failed to delete attachment.');
      console.error('Failed to delete attachment:', caughtError);
    } finally {
      setDeletingAttachmentId(null);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/20" onClick={onClose} />

      <div
        className="
          fixed right-0 top-0 z-50 h-full w-full overflow-y-auto border-l border-[var(--color-border)]
          bg-[var(--color-surface)] shadow-[-4px_0_12px_rgba(0,0,0,0.05)] md:w-[680px]
        "
      >
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-lg)] py-[var(--spacing-md)]">
          <div className="flex items-center gap-[var(--spacing-sm)]">
            <span className="font-mono text-[var(--text-xs)] text-[var(--color-text-muted)]">TASK-{taskId}</span>
          </div>
          <div className="flex items-center gap-[var(--spacing-sm)]">
            <button
              type="button"
              aria-label="Edit task"
              onClick={openEditModal}
              className="rounded-[var(--radius-sm)] border-none bg-transparent p-[var(--spacing-xs)] text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]"
            >
              <Edit size={14} />
            </button>
            <button
              type="button"
              aria-label="Delete task"
              onClick={handleTaskDelete}
              disabled={deletingTask}
              className="rounded-[var(--radius-sm)] border-none bg-transparent p-[var(--spacing-xs)] text-[var(--color-danger)] hover:bg-[var(--color-accent-red)] disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 size={14} />
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-[var(--radius-sm)] border-none bg-transparent p-[var(--spacing-xs)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {isEditing && task && (
          <div className="border-b border-[var(--color-border)] bg-[var(--color-surface-muted)] px-[var(--spacing-lg)] py-[var(--spacing-base)]">
            <div className="mb-[var(--spacing-sm)] flex items-center justify-between">
              <h3 className="m-0 text-[var(--text-base)] font-semibold text-[var(--color-text-primary)]">Edit Task</h3>
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-xs)] text-[var(--text-xs)] text-[var(--color-text-secondary)]"
              >
                Cancel
              </button>
            </div>
            <div className="grid gap-[var(--spacing-sm)] md:grid-cols-2">
              <div className="md:col-span-2">
                <label className="mb-[var(--spacing-xs)] block text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Task Title
                </label>
                <input
                  aria-label="Task Title"
                  value={editTitle}
                  onChange={(event) => setEditTitle(event.target.value)}
                  className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                />
              </div>
              <div className="md:col-span-2">
                <label className="mb-[var(--spacing-xs)] block text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Task Description
                </label>
                <textarea
                  aria-label="Task Description"
                  value={editDescription}
                  onChange={(event) => setEditDescription(event.target.value)}
                  rows={4}
                  className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                />
              </div>
              <div>
                <label className="mb-[var(--spacing-xs)] block text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Task Assignee
                </label>
                <select
                  aria-label="Task Assignee"
                  value={editAssigneeUserId}
                  onChange={(event) => setEditAssigneeUserId(event.target.value)}
                  className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                >
                  <option value="">Unassigned</option>
                  {projectMembers.map((member) => (
                    <option key={member.memberId} value={String(member.userId)}>
                      {member.nickname}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-[var(--spacing-xs)] block text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Priority
                </label>
                <select
                  aria-label="Task Priority"
                  value={editPriority}
                  onChange={(event) => setEditPriority(event.target.value as TaskPriority)}
                  className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                >
                  {priorityOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-[var(--spacing-xs)] block text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Due Date
                </label>
                <input
                  aria-label="Task Due Date"
                  type="date"
                  value={editDueDate}
                  onChange={(event) => setEditDueDate(event.target.value)}
                  className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                />
              </div>
              <div className="md:col-span-2">
                <div className="mb-[var(--spacing-xs)] block text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Labels
                </div>
                {projectLabels.length === 0 ? (
                  <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
                    No labels available.
                  </div>
                ) : (
                  <div className="flex flex-wrap gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-sm)]">
                    {projectLabels.map((label) => {
                      const checked = editLabelIds.includes(label.labelId);
                      return (
                        <label
                          key={label.labelId}
                          className="flex cursor-pointer items-center gap-[var(--spacing-xs)] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-[var(--spacing-sm)] py-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                        >
                          <input
                            type="checkbox"
                            aria-label={label.name}
                            checked={checked}
                            onChange={() => toggleEditLabel(label.labelId)}
                          />
                          <span
                            className="inline-flex h-[10px] w-[10px] rounded-full"
                            style={{ backgroundColor: label.colorHex }}
                          />
                          {label.name}
                        </label>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
            <div className="mt-[var(--spacing-sm)] flex justify-end">
              <button
                type="button"
                onClick={handleTaskSave}
                disabled={!editTitle.trim() || savingTask}
                className="h-[32px] rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {savingTask ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        )}

        {error && (
          <div
            className="
              mx-[var(--spacing-lg)] mt-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)]
              rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)]
              p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]
            "
          >
            <AlertCircle size={14} className="shrink-0" />
            {error}
          </div>
        )}

        {loading ? (
          <div className="space-y-4 p-[var(--spacing-lg)] animate-pulse">
            <div className="h-6 w-3/4 rounded bg-[var(--color-surface-muted)]" />
            <div className="h-4 w-1/2 rounded bg-[var(--color-surface-muted)]" />
            <div className="h-32 rounded bg-[var(--color-surface-muted)]" />
          </div>
        ) : task ? (
          <div className="flex flex-col md:flex-row">
            <div className="min-w-0 flex-1 border-[var(--color-border)] p-[var(--spacing-lg)] md:border-r">
              <h2 className="m-0 mb-[var(--spacing-xs)] text-[var(--text-lg)] font-bold text-[var(--color-text-primary)]">
                {task.title}
              </h2>
              <div className="mb-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                opened by {task.creator.nickname} · {formatTimeAgo(task.createdAt)} · updated {formatTimeAgo(task.updatedAt)}
              </div>

              <div className="mb-[var(--spacing-lg)]">
                <h3 className="m-0 mb-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  Description
                </h3>
                <div
                  className="
                    rounded-[var(--radius-sm)] border border-[var(--color-border-muted)]
                    bg-[var(--color-surface-muted)] p-[var(--spacing-base)] text-[var(--text-sm)]
                    leading-relaxed text-[var(--color-text-secondary)]
                  "
                >
                  {task.description || 'No description.'}
                </div>
              </div>

              <div>
                <h3 className="m-0 mb-[var(--spacing-md)] flex items-center gap-[var(--spacing-xs)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  <MessageSquare size={14} />
                  Activity ({comments.length})
                </h3>

                {comments.length === 0 && (
                  <div className="mb-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
                    No comments yet.
                  </div>
                )}

                <div className="mb-[var(--spacing-lg)] space-y-[var(--spacing-md)]">
                  {comments.map((comment) => (
                    <div key={comment.commentId} className="flex gap-[var(--spacing-sm)]">
                      <div
                        className="
                          mt-[2px] flex h-[24px] w-[24px] shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)]
                          text-[10px] font-semibold text-white
                        "
                      >
                        {comment.author.nickname.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-sm)]">
                          <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                            {comment.author.nickname}
                          </span>
                          <span className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                            {formatTimeAgo(comment.createdAt)}
                          </span>
                        </div>
                        <div
                          className="
                            rounded-[var(--radius-sm)] border border-[var(--color-border-muted)]
                            bg-[var(--color-surface-muted)] p-[var(--spacing-sm)] text-[var(--text-sm)]
                            text-[var(--color-text-secondary)]
                          "
                        >
                          {comment.content}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="border-t border-[var(--color-border)] pt-[var(--spacing-md)]">
                  <textarea
                    aria-label="Comment"
                    value={newComment}
                    onChange={(event) => setNewComment(event.target.value)}
                    placeholder="Write a comment..."
                    rows={3}
                    className="
                      w-full resize-vertical rounded-[var(--radius-sm)] border border-[var(--color-border)]
                      bg-[var(--color-surface)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                      placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
                    "
                  />
                  <div className="mt-[var(--spacing-sm)] flex justify-end">
                    <button
                      onClick={handleCommentSubmit}
                      disabled={!newComment.trim() || submitting}
                      className="
                        h-[32px] rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)]
                        px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
                        hover:bg-[var(--color-primary-hover)] disabled:cursor-not-allowed disabled:opacity-50
                      "
                    >
                      {submitting ? 'Sending...' : 'Comment'}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div className="w-full shrink-0 space-y-[var(--spacing-base)] border-[var(--color-border)] p-[var(--spacing-base)] md:w-[220px] md:border-t-0 border-t">
              <div>
                <div className="mb-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Status
                </div>
                <select
                  value={task.status}
                  onChange={(event) => handleStatusChange(event.target.value)}
                  className="
                    h-[28px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
                    bg-[var(--color-surface)] px-[var(--spacing-xs)] text-[var(--text-sm)]
                    focus:border-[var(--color-primary)] focus:outline-none
                  "
                >
                  {statusOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Assignee
                </div>
                {task.assignee ? (
                  <div className="flex items-center gap-[var(--spacing-xs)]">
                    <div
                      className="
                        flex h-[20px] w-[20px] items-center justify-center rounded-full bg-[var(--color-primary)]
                        text-[10px] font-semibold text-white
                      "
                    >
                      {task.assignee.nickname.charAt(0).toUpperCase()}
                    </div>
                    <span className="text-[var(--text-sm)] text-[var(--color-text-primary)]">
                      {task.assignee.nickname}
                    </span>
                  </div>
                ) : (
                  <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">Unassigned</span>
                )}
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Priority
                </div>
                <Badge variant={priorityVariant[task.priority] || 'muted'}>{task.priority}</Badge>
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Due Date
                </div>
                <span className="text-[var(--text-sm)] text-[var(--color-text-primary)]">
                  {task.dueDate ? formatDate(task.dueDate) : '-'}
                </span>
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Creator
                </div>
                <span className="text-[var(--text-sm)] text-[var(--color-text-primary)]">{task.creator.nickname}</span>
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  Labels
                </div>
                {task.labels.length > 0 ? (
                  <div className="flex flex-wrap gap-1">
                    {task.labels.map((label) => (
                      <span
                        key={label.labelId}
                        className="inline-block rounded-[var(--radius-sm)] border px-[5px] py-[1px] text-[11px] font-medium"
                        style={{
                          backgroundColor: `${label.colorHex}20`,
                          borderColor: `${label.colorHex}40`,
                          color: label.colorHex,
                        }}
                      >
                        {label.name}
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">None</span>
                )}
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  <Paperclip size={11} />
                  Attachments ({attachments.length})
                </div>
                {attachments.length > 0 ? (
                  <div className="space-y-2">
                    {attachments.map((file) => (
                      <div key={file.attachmentId} className="rounded-[var(--radius-sm)] border border-[var(--color-border-muted)] p-[var(--spacing-xs)]">
                        <div className="text-[var(--text-xs)] text-[var(--color-text-secondary)]">
                          <span className="font-medium text-[var(--color-primary)]">{file.originalFilename}</span>
                          <br />
                          <span className="text-[var(--color-text-muted)]">
                            {file.uploaderNickname} · {(file.sizeBytes / 1024).toFixed(1)}KB
                          </span>
                        </div>
                        {user?.userId === file.uploaderUserId && (
                          <button
                            type="button"
                            aria-label={`Delete attachment ${file.originalFilename}`}
                            onClick={() => handleAttachmentDelete(file.attachmentId)}
                            disabled={deletingAttachmentId === file.attachmentId}
                            className="
                              mt-[var(--spacing-xs)] rounded-[var(--radius-sm)] border border-[var(--color-danger)]
                              bg-transparent px-[var(--spacing-xs)] py-[2px] text-[11px] font-medium text-[var(--color-danger)]
                              hover:bg-[var(--color-accent-red)] disabled:cursor-not-allowed disabled:opacity-50
                            "
                          >
                            {deletingAttachmentId === file.attachmentId ? 'Deleting...' : 'Delete'}
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">None</span>
                )}
              </div>

              <div>
                <div className="mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-xs)] text-[var(--text-xs)] font-semibold uppercase text-[var(--color-text-muted)]">
                  <Clock size={11} />
                  Status History
                </div>
                {task.recentStatusHistories.length > 0 ? (
                  <div className="space-y-1">
                    {task.recentStatusHistories.map((history) => (
                      <div key={history.historyId} className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        <span className="font-medium text-[var(--color-text-secondary)]">{history.changedBy.nickname}</span>{' '}
                        {history.fromStatus} {'->'} {history.toStatus}
                        <br />
                        <span>{formatTimeAgo(history.changedAt)}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <span className="text-[var(--text-xs)] text-[var(--color-text-muted)]">No status history</span>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="p-[var(--spacing-lg)] text-[var(--color-text-muted)]">Task could not be loaded.</div>
        )}
      </div>
    </>
  );
}
