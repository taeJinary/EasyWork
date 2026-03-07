import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Plus, Users, FolderKanban, ChevronRight } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import FilterBar from '@/components/FilterBar';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  WorkspaceListItemResponse,
  WorkspaceListResponse,
  WorkspaceSummaryResponse,
  WorkspaceSummary,
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

function normalizeWorkspaces(items: WorkspaceListItemResponse[] | undefined): WorkspaceSummary[] {
  if (!Array.isArray(items)) {
    return [];
  }

  return items.map((item) => ({
    id: item.workspaceId,
    name: item.name,
    description: item.description,
    updatedAt: item.updatedAt,
    memberCount: item.memberCount,
  }));
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
        setWorkspaces(normalizeWorkspaces(res.data.data.content));
      } catch {
        setError('워크스페이스 목록을 불러오는 데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchWorkspaces();
  }, []);

  const filteredWorkspaces = workspaces.filter((ws) =>
    ws.name.toLowerCase().includes(searchQuery.toLowerCase())
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
        description="팀 작업 공간을 관리하고 활발한 작업으로 돌아가세요."
        actions={
          <button className="
            flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
            bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
            text-[var(--text-sm)] font-medium border-none cursor-pointer
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
        searchPlaceholder="워크스페이스 검색..."
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
      >
        <select className="
          h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
          rounded-[var(--radius-sm)] bg-[var(--color-surface)]
          text-[var(--text-sm)] text-[var(--color-text-secondary)]
          focus:outline-none focus:border-[var(--color-primary)]
        ">
          <option value="updated">Sort: Updated</option>
          <option value="name">Sort: Name</option>
          <option value="created">Sort: Created</option>
        </select>
      </FilterBar>

      {/* Error state */}
      {error && (
        <div className="
          p-[var(--spacing-base)] bg-[var(--color-accent-red)]
          text-[var(--color-danger)] text-[var(--text-sm)]
          rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20
        ">
          {error}
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center p-[var(--spacing-base)] border-b border-[var(--color-border)] last:border-b-0 animate-pulse">
              <div className="flex-1">
                <div className="h-4 bg-[var(--color-surface-muted)] rounded w-48 mb-2" />
                <div className="h-3 bg-[var(--color-surface-muted)] rounded w-32" />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && filteredWorkspaces.length === 0 && (
        <div className="
          text-center py-[var(--spacing-xl)]
          text-[var(--color-text-muted)] text-[var(--text-sm)]
        ">
          {searchQuery
            ? '검색 결과가 없습니다.'
            : '아직 워크스페이스가 없습니다. 첫 워크스페이스를 생성하세요.'}
        </div>
      )}

      {/* Workspace list */}
      {!loading && !error && filteredWorkspaces.length > 0 && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
          {filteredWorkspaces.map((ws, index) => (
            <div
              key={ws.id}
              onClick={() => navigate(`/workspaces/${ws.id}`)}
              className={`
                flex items-center px-[var(--spacing-base)] py-[var(--spacing-md)]
                cursor-pointer hover:bg-[var(--color-surface-muted)] transition-colors
                ${index < filteredWorkspaces.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
              `}
            >
              {/* Icon */}
              <div className="
                w-[32px] h-[32px] rounded-[var(--radius-sm)]
                bg-[var(--color-surface-muted)] flex items-center justify-center
                mr-[var(--spacing-md)] shrink-0
              ">
                <FolderKanban size={16} className="text-[var(--color-text-muted)]" />
              </div>

              {/* Info */}
              <div className="flex-1 min-w-0">
                <div className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {ws.name}
                </div>
                {ws.description && (
                  <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] truncate">
                    {ws.description}
                  </div>
                )}
              </div>

              {/* Meta info */}
              <div className="flex items-center gap-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)] shrink-0">
                <span className="flex items-center gap-1">
                  <Users size={12} />
                  {ws.memberCount ?? 0} members
                </span>
                <span className="flex items-center gap-1">
                  <FolderKanban size={12} />
                  projects
                </span>
                <span>Updated {formatTimeAgo(ws.updatedAt)}</span>
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
              className="w-full max-w-[480px] bg-[var(--color-surface)] rounded-[var(--radius-md)] border border-[var(--color-border)] shadow-[var(--shadow-sm)] p-[var(--spacing-lg)]"
            >
              <div className="flex items-start justify-between gap-[var(--spacing-base)] mb-[var(--spacing-base)]">
                <div>
                  <h2 className="text-[var(--text-lg)] font-semibold text-[var(--color-text-primary)] m-0">
                    Create Workspace
                  </h2>
                  <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] mb-0">
                    Create a workspace to group projects and members.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={closeCreateModal}
                  className="h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)] rounded-[var(--radius-sm)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] cursor-pointer"
                >
                  Close
                </button>
              </div>

              <div className="mb-[var(--spacing-base)]">
                <label
                  htmlFor="workspace-name"
                  className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]"
                >
                  Workspace Name
                </label>
                <input
                  id="workspace-name"
                  type="text"
                  value={createName}
                  onChange={(event) => setCreateName(event.target.value)}
                  maxLength={100}
                  className="w-full h-[36px] px-[var(--spacing-sm)] border border-[var(--color-border)] rounded-[var(--radius-sm)] bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]"
                />
              </div>

              <div className="mb-[var(--spacing-base)]">
                <label
                  htmlFor="workspace-description"
                  className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]"
                >
                  Description
                </label>
                <textarea
                  id="workspace-description"
                  value={createDescription}
                  onChange={(event) => setCreateDescription(event.target.value)}
                  maxLength={500}
                  rows={4}
                  className="w-full px-[var(--spacing-sm)] py-[var(--spacing-sm)] border border-[var(--color-border)] rounded-[var(--radius-sm)] bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)] resize-none focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]"
                />
              </div>

              {createError && (
                <div className="mb-[var(--spacing-base)] p-[var(--spacing-sm)] bg-[var(--color-accent-red)] text-[var(--color-danger)] text-[var(--text-sm)] rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20">
                  {createError}
                </div>
              )}

              <div className="flex items-center justify-end gap-[var(--spacing-sm)]">
                <button
                  type="button"
                  onClick={closeCreateModal}
                  className="h-[36px] px-[var(--spacing-base)] border border-[var(--color-border)] rounded-[var(--radius-sm)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="h-[36px] px-[var(--spacing-base)] bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)] text-[var(--text-sm)] font-medium border-none cursor-pointer hover:bg-[var(--color-primary-hover)] disabled:opacity-50 disabled:cursor-not-allowed"
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
