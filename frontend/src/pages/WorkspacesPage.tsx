import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronRight, FolderKanban, Plus, Users } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import FilterBar from '@/components/FilterBar';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  WorkspaceListItemResponse,
  WorkspaceListResponse,
  WorkspaceSummary,
  WorkspaceSummaryResponse,
} from '@/types';

function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function toWorkspaceSummary(workspace: WorkspaceListItemResponse): WorkspaceSummary {
  return {
    id: workspace.workspaceId,
    name: workspace.name,
    description: workspace.description,
    myRole: workspace.myRole,
    memberCount: workspace.memberCount,
    updatedAt: workspace.updatedAt,
  };
}

export default function WorkspacesPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [createName, setCreateName] = useState('');
  const [createDescription, setCreateDescription] = useState('');
  const [createError, setCreateError] = useState('');
  const [creating, setCreating] = useState(false);

  const showCreateModal = searchParams.get('create') === 'workspace';

  useEffect(() => {
    async function fetchWorkspaces() {
      try {
        setLoading(true);
        const res = await apiClient.get<ApiResponse<WorkspaceListResponse>>('/workspaces');
        setWorkspaces(res.data.data.content.map(toWorkspaceSummary));
      } catch {
        setError('Failed to load workspaces.');
      } finally {
        setLoading(false);
      }
    }

    void fetchWorkspaces();
  }, []);

  const filteredWorkspaces = workspaces.filter((workspace) =>
    workspace.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const openCreateModal = () => {
    const next = new URLSearchParams(searchParams);
    next.set('create', 'workspace');
    setSearchParams(next);
    setCreateError('');
  };

  const closeCreateModal = () => {
    const next = new URLSearchParams(searchParams);
    next.delete('create');
    setSearchParams(next);
    setCreateName('');
    setCreateDescription('');
    setCreateError('');
  };

  const handleCreateWorkspace = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedName = createName.trim();
    const normalizedDescription = createDescription.trim();

    if (!normalizedName) {
      setCreateError('Workspace name is required.');
      return;
    }

    try {
      setCreating(true);
      setCreateError('');

      const response = await apiClient.post<ApiResponse<WorkspaceSummaryResponse>>('/workspaces', {
        name: normalizedName,
        description: normalizedDescription || undefined,
      });

      setWorkspaces((prev) => [
        {
          id: response.data.data.workspaceId,
          name: response.data.data.name,
          description: response.data.data.description,
          myRole: response.data.data.myRole,
          updatedAt: new Date().toISOString(),
          memberCount: 1,
        },
        ...prev,
      ]);
      closeCreateModal();
    } catch {
      setCreateError('Failed to create workspace.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Workspaces"
        description="Manage your collaboration spaces and move into project work from here."
        actions={
          <button
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border-none
              bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
              hover:bg-[var(--color-primary-hover)]
            "
            onClick={openCreateModal}
          >
            <Plus size={16} />
            New Workspace
          </button>
        }
      />

      <FilterBar
        searchPlaceholder="Search workspaces..."
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
      >
        <select
          className="
            h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:border-[var(--color-primary)] focus:outline-none
          "
        >
          <option value="updated">Sort: Updated</option>
          <option value="name">Sort: Name</option>
          <option value="created">Sort: Created</option>
        </select>
      </FilterBar>

      {error && (
        <div
          className="
            rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20 bg-[var(--color-accent-red)]
            p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-danger)]
          "
        >
          {error}
        </div>
      )}

      {loading && (
        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {[1, 2, 3].map((item) => (
            <div
              key={item}
              className="animate-pulse border-b border-[var(--color-border)] p-[var(--spacing-base)] last:border-b-0"
            >
              <div className="mb-2 h-4 w-48 rounded bg-[var(--color-surface-muted)]" />
              <div className="h-3 w-32 rounded bg-[var(--color-surface-muted)]" />
            </div>
          ))}
        </div>
      )}

      {!loading && !error && filteredWorkspaces.length === 0 && (
        <div className="py-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
          {searchQuery
            ? 'No workspaces matched your search.'
            : 'No workspaces yet. Create your first workspace.'}
        </div>
      )}

      {!loading && !error && filteredWorkspaces.length > 0 && (
        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {filteredWorkspaces.map((workspace, index) => (
            <div
              key={workspace.id}
              onClick={() => navigate(`/workspaces/${workspace.id}`)}
              className={`
                flex cursor-pointer items-center px-[var(--spacing-base)] py-[var(--spacing-md)]
                transition-colors hover:bg-[var(--color-surface-muted)]
                ${index < filteredWorkspaces.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
              `}
            >
              <div
                className="
                  mr-[var(--spacing-md)] flex h-[32px] w-[32px] shrink-0 items-center justify-center
                  rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)]
                "
              >
                <FolderKanban size={16} className="text-[var(--color-text-muted)]" />
              </div>

              <div className="min-w-0 flex-1">
                <div className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {workspace.name}
                </div>
                {workspace.description && (
                  <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                    {workspace.description}
                  </div>
                )}
              </div>

              <div className="flex shrink-0 items-center gap-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                <span className="flex items-center gap-1">
                  <Users size={12} />
                  {workspace.memberCount ?? 0} members
                </span>
                <span className="flex items-center gap-1">
                  <FolderKanban size={12} />
                  {workspace.myRole}
                </span>
                <span>Updated {formatTimeAgo(workspace.updatedAt)}</span>
                <ChevronRight size={16} className="text-[var(--color-text-muted)]" />
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreateModal && (
        <div
          data-testid="workspace-create-backdrop"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-[var(--spacing-base)]"
          onClick={closeCreateModal}
        >
          <form
            role="dialog"
            aria-modal="true"
            onSubmit={handleCreateWorkspace}
            onClick={(event) => event.stopPropagation()}
            className="
              w-full max-w-[480px] rounded-[var(--radius-md)] border border-[var(--color-border)]
              bg-[var(--color-surface)] p-[var(--spacing-lg)] shadow-[var(--shadow-sm)]
            "
          >
            <div className="mb-[var(--spacing-base)] flex items-start justify-between gap-[var(--spacing-base)]">
              <div>
                <h2 className="m-0 text-[var(--text-lg)] font-semibold text-[var(--color-text-primary)]">
                  Create Workspace
                </h2>
                <p className="mb-0 mt-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
                  Create a workspace to group projects and members.
                </p>
              </div>
              <button
                type="button"
                onClick={closeCreateModal}
                className="
                  h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
                  bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--color-text-secondary)]
                "
              >
                Close
              </button>
            </div>

            <div className="mb-[var(--spacing-base)]">
              <label
                htmlFor="workspace-name"
                className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
              >
                Workspace Name
              </label>
              <input
                id="workspace-name"
                type="text"
                value={createName}
                onChange={(event) => setCreateName(event.target.value)}
                maxLength={100}
                className="
                  h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
                  bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                  focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
                "
              />
            </div>

            <div className="mb-[var(--spacing-base)]">
              <label
                htmlFor="workspace-description"
                className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
              >
                Description
              </label>
              <textarea
                id="workspace-description"
                value={createDescription}
                onChange={(event) => setCreateDescription(event.target.value)}
                maxLength={500}
                rows={4}
                className="
                  w-full resize-none rounded-[var(--radius-sm)] border border-[var(--color-border)]
                  bg-[var(--color-surface)] px-[var(--spacing-sm)] py-[var(--spacing-sm)] text-[var(--text-sm)]
                  text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
                "
              />
            </div>

            {createError && (
              <div className="mb-[var(--spacing-base)] rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20 bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                {createError}
              </div>
            )}

            <div className="flex items-center justify-end gap-[var(--spacing-sm)]">
              <button
                type="button"
                onClick={closeCreateModal}
                className="
                  h-[36px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
                  bg-[var(--color-surface)] px-[var(--spacing-base)] text-[var(--color-text-secondary)]
                "
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={creating}
                className="
                  h-[36px] rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)]
                  px-[var(--spacing-base)] text-[var(--text-sm)] font-medium text-white
                  hover:bg-[var(--color-primary-hover)] disabled:cursor-not-allowed disabled:opacity-50
                "
              >
                {creating ? 'Creating...' : 'Create Workspace'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
