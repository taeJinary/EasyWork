import { useEffect, useState } from 'react';
import { AlertCircle, X } from 'lucide-react';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  ProjectCreateResponse,
  WorkspaceListResponse,
  WorkspaceSummary,
  WorkspaceListItemResponse,
} from '@/types';

type ProjectCreateModalProps = {
  fixedWorkspaceId?: number;
  open: boolean;
  onClose: () => void;
  onCreated: (project: ProjectCreateResponse) => void;
};

function toWorkspaceSummary(item: WorkspaceListItemResponse): WorkspaceSummary {
  return {
    id: item.workspaceId,
    name: item.name,
    description: item.description,
    myRole: item.myRole,
    memberCount: item.memberCount,
    updatedAt: item.updatedAt,
  };
}

export default function ProjectCreateModal({
  fixedWorkspaceId,
  open,
  onClose,
  onCreated,
}: ProjectCreateModalProps) {
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [workspaceId, setWorkspaceId] = useState(fixedWorkspaceId ? String(fixedWorkspaceId) : '');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loadingWorkspaces, setLoadingWorkspaces] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    setWorkspaceId(fixedWorkspaceId ? String(fixedWorkspaceId) : '');
    setName('');
    setDescription('');
    setError(null);

    if (fixedWorkspaceId) {
      return;
    }

    let cancelled = false;

    async function fetchWorkspaces() {
      try {
        setLoadingWorkspaces(true);
        const response = await apiClient.get<ApiResponse<WorkspaceListResponse>>('/workspaces');
        if (cancelled) {
          return;
        }
        setWorkspaces(response.data.data.content.map(toWorkspaceSummary));
      } catch (caughtError) {
        if (cancelled) {
          return;
        }
        setError('Failed to load workspaces.');
        console.error('Failed to load workspaces for project creation:', caughtError);
      } finally {
        if (!cancelled) {
          setLoadingWorkspaces(false);
        }
      }
    }

    void fetchWorkspaces();

    return () => {
      cancelled = true;
    };
  }, [fixedWorkspaceId, open]);

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
    if (!workspaceId || !name.trim()) {
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const response = await apiClient.post<ApiResponse<ProjectCreateResponse>>('/projects', {
        workspaceId: Number(workspaceId),
        name: name.trim(),
        description: description.trim(),
      });
      onCreated(response.data.data);
      onClose();
    } catch (caughtError: unknown) {
      const message = (caughtError as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(message || 'Failed to create project.');
      console.error('Failed to create project:', caughtError);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" data-testid="project-create-backdrop" onClick={handleClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-[var(--spacing-base)]" onClick={handleClose}>
        <div
          className="w-full max-w-[480px] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-lg)] shadow-lg"
          onClick={(event) => event.stopPropagation()}
          role="dialog"
          aria-modal="true"
        >
          <div className="mb-[var(--spacing-base)] flex items-center justify-between">
            <h3 className="m-0 text-[var(--text-base)] font-bold text-[var(--color-text-primary)]">Create Project</h3>
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
            {!fixedWorkspaceId && (
              <div>
                <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                  Workspace
                </label>
                <select
                  aria-label="Workspace"
                  value={workspaceId}
                  onChange={(event) => setWorkspaceId(event.target.value)}
                  disabled={loadingWorkspaces}
                  className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
                >
                  <option value="">Select workspace</option>
                  {workspaces.map((workspace) => (
                    <option key={workspace.id} value={String(workspace.id)}>
                      {workspace.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                Project Name
              </label>
              <input
                aria-label="Project Name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
              />
            </div>

            <div>
              <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                Description
              </label>
              <textarea
                aria-label="Description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                className="min-h-[96px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-sm)] text-[var(--text-sm)]"
              />
            </div>

            <div className="flex justify-end gap-[var(--spacing-sm)]">
              <button
                type="button"
                onClick={handleClose}
                className="h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-secondary)]"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={!workspaceId || !name.trim() || submitting || loadingWorkspaces}
                className="h-[32px] rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white disabled:opacity-50"
              >
                Create Project
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
