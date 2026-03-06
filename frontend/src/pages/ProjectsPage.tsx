import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, ChevronLeft, ChevronRight } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
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

  useEffect(() => {
    async function fetchProjects() {
      try {
        setLoading(true);
        const params: Record<string, string | number> = { page, size: 20 };
        if (searchQuery) params.keyword = searchQuery;
        if (roleFilter) params.role = roleFilter;

        const res = await apiClient.get<ApiResponse<ProjectListResponse>>('/projects', { params });
        setProjects(res.data.data.projects);
        setTotalPages(res.data.data.pageInfo.totalPages);
      } catch {
        setError('프로젝트 목록을 불러오는 데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    }
    fetchProjects();
  }, [page, searchQuery, roleFilter]);

  return (
    <div>
      <PageHeader
        title="Projects"
        description="참여 중이거나 소유한 프로젝트를 확인하세요."
        actions={
          <button className="
            flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
            bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
            text-[var(--text-sm)] font-medium border-none cursor-pointer
            hover:bg-[var(--color-primary-hover)]
          ">
            <Plus size={16} />
            New Project
          </button>
        }
      />

      <FilterBar
        searchPlaceholder="프로젝트 검색..."
        searchValue={searchQuery}
        onSearchChange={(v) => { setSearchQuery(v); setPage(0); }}
      >
        <select
          value={roleFilter}
          onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
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
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="flex items-center p-[var(--spacing-base)] border-b border-[var(--color-border)] last:border-b-0 animate-pulse">
              <div className="flex-1">
                <div className="h-4 bg-[var(--color-surface-muted)] rounded w-56 mb-2" />
                <div className="h-3 bg-[var(--color-surface-muted)] rounded w-40" />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && projects.length === 0 && (
        <div className="
          text-center py-[var(--spacing-xl)]
          text-[var(--color-text-muted)] text-[var(--text-sm)]
        ">
          {searchQuery || roleFilter
            ? '검색 결과가 없습니다.'
            : '아직 프로젝트가 없습니다. 첫 프로젝트를 생성하세요.'}
        </div>
      )}

      {/* Project list */}
      {!loading && !error && projects.length > 0 && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
          {projects.map((project, index) => (
            <div
              key={project.id}
              onClick={() => navigate(`/projects/${project.id}/board`)}
              className={`
                flex items-center px-[var(--spacing-base)] py-[var(--spacing-md)]
                cursor-pointer hover:bg-[var(--color-surface-muted)] transition-colors
                ${index < projects.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
              `}
            >
              {/* Project name + role */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-[var(--spacing-sm)]">
                  <span className="text-[var(--text-sm)] font-semibold text-[var(--color-primary)]">
                    {project.name}
                  </span>
                  <Badge variant={project.myRole === 'OWNER' ? 'warning' : 'default'} size="sm">
                    {project.myRole}
                  </Badge>
                </div>
                {project.description && (
                  <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] truncate mt-[2px]">
                    {project.description}
                  </div>
                )}
              </div>

              {/* Meta info */}
              <div className="flex items-center gap-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)] shrink-0">
                <span>{project.memberCount} members</span>
                <span>{project.openTaskCount} open tasks</span>
                <span>Updated {formatTimeAgo(project.updatedAt)}</span>
              </div>
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
          <span className="text-[var(--color-text-muted)]">
            Page {page + 1} / {totalPages}
          </span>
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
