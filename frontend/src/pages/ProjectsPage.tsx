import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import ProjectCreateModal from '@/components/ProjectCreateModal';
import apiClient from '@/api/client';
import { toProjectSummary } from '@/utils/projectMappers';
import type { ApiResponse, ProjectListResponse, ProjectSummary } from '@/types';

function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export default function ProjectsPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);

  useEffect(() => {
    async function fetchProjects() {
      try {
        setLoading(true);
        const params: Record<string, string | number> = { page, size: 20 };
        if (searchQuery) params.keyword = searchQuery;
        if (roleFilter) params.role = roleFilter;

        const res = await apiClient.get<ApiResponse<ProjectListResponse>>('/projects', { params });
        setProjects(res.data.data.content.map(toProjectSummary));
        setTotalPages(res.data.data.totalPages);
      } catch {
        setError('Failed to load projects.');
      } finally {
        setLoading(false);
      }
    }
    void fetchProjects();
  }, [page, searchQuery, roleFilter]);

  return (
    <div>
      <PageHeader
        title="Projects"
        description="참여 중이거나 소유한 프로젝트를 확인하세요."
        actions={
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="
              flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
              bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
              text-[var(--text-sm)] font-medium border-none cursor-pointer
              hover:bg-[var(--color-primary-hover)]
            "
          >
            <Plus size={16} />
            New Project
          </button>
        }
      />

      <ProjectCreateModal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreated={(project) => navigate(`/projects/${project.projectId}/board`)}
      />

      <FilterBar
        searchPlaceholder="프로젝트 검색..."
        searchValue={searchQuery}
        onSearchChange={(value) => {
          setSearchQuery(value);
          setPage(0);
        }}
      >
        <select
          value={roleFilter}
          onChange={(event) => {
            setRoleFilter(event.target.value);
            setPage(0);
          }}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="">Role: All</option>
          <option value="OWNER">Role: Owner</option>
          <option value="MEMBER">Role: Member</option>
        </select>
        <select
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="updated">Sort: Updated</option>
          <option value="name">Sort: Name</option>
          <option value="created">Sort: Created</option>
        </select>
      </FilterBar>

      {error && (
        <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20 bg-[var(--color-accent-red)] p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-danger)]">
          {error}
        </div>
      )}

      {loading && (
        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {[1, 2, 3, 4].map((item) => (
            <div key={item} className="flex animate-pulse items-center border-b border-[var(--color-border)] p-[var(--spacing-base)] last:border-b-0">
              <div className="flex-1">
                <div className="mb-2 h-4 w-56 rounded bg-[var(--color-surface-muted)]" />
                <div className="h-3 w-40 rounded bg-[var(--color-surface-muted)]" />
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && !error && projects.length === 0 && (
        <div className="py-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
          {searchQuery || roleFilter ? '검색 결과가 없습니다.' : '아직 프로젝트가 없습니다. 첫 프로젝트를 생성하세요.'}
        </div>
      )}

      {!loading && !error && projects.length > 0 && (
        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {projects.map((project, index) => (
            <div
              key={project.id}
              onClick={() => navigate(`/projects/${project.id}/board`)}
              className={`
                flex cursor-pointer items-center px-[var(--spacing-base)] py-[var(--spacing-md)] transition-colors hover:bg-[var(--color-surface-muted)]
                ${index < projects.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
              `}
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-[var(--spacing-sm)]">
                  <span className="text-[var(--text-sm)] font-semibold text-[var(--color-primary)]">{project.name}</span>
                  <Badge variant={project.myRole === 'OWNER' ? 'warning' : 'default'} size="sm">
                    {project.myRole}
                  </Badge>
                </div>
                {project.description && (
                  <div className="mt-[2px] truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                    {project.description}
                  </div>
                )}
              </div>

              <div className="flex shrink-0 items-center gap-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                <span>{project.memberCount} members</span>
                <span>{project.openTaskCount} open tasks</span>
                <span>Updated {formatTimeAgo(project.updatedAt)}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="mt-[var(--spacing-base)] flex items-center justify-between text-[var(--text-sm)]">
          <button
            onClick={() => setPage((currentPage) => Math.max(0, currentPage - 1))}
            disabled={page === 0}
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
              bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--color-text-secondary)]
              hover:bg-[var(--color-surface-muted)] disabled:cursor-not-allowed disabled:opacity-50
            "
          >
            <ChevronLeft size={14} />
            Prev
          </button>
          <span className="text-[var(--color-text-muted)]">Page {page + 1} / {totalPages}</span>
          <button
            onClick={() => setPage((currentPage) => Math.min(totalPages - 1, currentPage + 1))}
            disabled={page >= totalPages - 1}
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
              bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--color-text-secondary)]
              hover:bg-[var(--color-surface-muted)] disabled:cursor-not-allowed disabled:opacity-50
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
