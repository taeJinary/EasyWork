import { useEffect, useState } from 'react';
import { AlertCircle, X } from 'lucide-react';
import apiClient from '@/api/client';
import type { ApiResponse, ProjectLabelResponse, TaskCreateResponse, TaskPriority } from '@/types';

type TaskCreateModalProps = {
  projectId: number;
  open: boolean;
  onClose: () => void;
  onCreated: (task: TaskCreateResponse) => void;
};

const priorityOptions: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export default function TaskCreateModal({ projectId, open, onClose, onCreated }: TaskCreateModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [labels, setLabels] = useState<ProjectLabelResponse[]>([]);
  const [selectedLabelIds, setSelectedLabelIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setTitle('');
    setDescription('');
    setPriority('MEDIUM');
    setLabels([]);
    setSelectedLabelIds([]);
    setError(null);
  }, [open, projectId]);

  useEffect(() => {
    if (!open) {
      return;
    }

    let cancelled = false;

    async function fetchLabels() {
      try {
        const response = await apiClient.get<ApiResponse<ProjectLabelResponse[]>>(`/projects/${projectId}/labels`);
        if (!cancelled) {
          setLabels(response.data.data);
        }
      } catch (caughtError: unknown) {
        if (!cancelled) {
          const message = (caughtError as { response?: { data?: { message?: string } } })?.response?.data?.message;
          setError(message || '라벨을 불러오지 못했습니다.');
        }
      }
    }

    void fetchLabels();

    return () => {
      cancelled = true;
    };
  }, [open, projectId]);

  if (!open) {
    return null;
  }

  const handleClose = () => {
    if (submitting) {
      return;
    }
    onClose();
  };

  const handleSubmit = async () => {
    if (!title.trim()) {
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const response = await apiClient.post<ApiResponse<TaskCreateResponse>>(`/projects/${projectId}/tasks`, {
        title: title.trim(),
        description: description.trim(),
        priority,
        labelIds: selectedLabelIds,
      });
      onCreated(response.data.data);
      onClose();
    } catch (caughtError: unknown) {
      const message = (caughtError as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(message || '작업을 생성하지 못했습니다.');
      console.error('Failed to create task:', caughtError);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleLabel = (labelId: number) => {
    setSelectedLabelIds((current) =>
      current.includes(labelId) ? current.filter((id) => id !== labelId) : [...current, labelId]
    );
  };

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" data-testid="task-create-backdrop" onClick={handleClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-[var(--spacing-base)]" onClick={handleClose}>
        <div
          className="w-full max-w-[480px] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-lg)] shadow-lg"
          onClick={(event) => event.stopPropagation()}
          role="dialog"
          aria-modal="true"
        >
          <div className="mb-[var(--spacing-base)] flex items-center justify-between">
            <h3 className="m-0 text-[var(--text-base)] font-bold text-[var(--color-text-primary)]">작업 생성</h3>
            <button
              type="button"
              onClick={handleClose}
              className="border-none bg-transparent p-[var(--spacing-xs)] text-[var(--color-text-muted)]"
            >
              <X size={16} />
            </button>
          </div>

          {error && (
            <div className="mb-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
              <AlertCircle size={14} />
              {error}
            </div>
          )}

          <div className="space-y-[var(--spacing-md)]">
            <div>
              <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                작업 제목
              </label>
              <input
                aria-label="작업 제목"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
              />
            </div>

            <div>
              <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                설명
              </label>
              <textarea
                aria-label="설명"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                className="min-h-[96px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-sm)] text-[var(--text-sm)]"
              />
            </div>

            <div>
              <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                우선순위
              </label>
              <select
                aria-label="우선순위"
                value={priority}
                onChange={(event) => setPriority(event.target.value as TaskPriority)}
                className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
              >
                {priorityOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                라벨
              </div>
              {labels.length === 0 ? (
                <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-[var(--spacing-sm)] py-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
                  사용할 수 있는 라벨이 없습니다.
                </div>
              ) : (
                <div className="flex flex-wrap gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-[var(--spacing-sm)]">
                  {labels.map((label) => {
                    const checked = selectedLabelIds.includes(label.labelId);
                    return (
                      <label
                        key={label.labelId}
                        className="flex cursor-pointer items-center gap-[var(--spacing-xs)] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-primary)]"
                      >
                        <input
                          type="checkbox"
                          aria-label={label.name}
                          checked={checked}
                          onChange={() => toggleLabel(label.labelId)}
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

            <div className="flex justify-end gap-[var(--spacing-sm)]">
              <button
                type="button"
                onClick={handleClose}
                className="h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-secondary)]"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={!title.trim() || submitting}
                className="h-[32px] rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white disabled:opacity-50"
              >
                작업 생성
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
