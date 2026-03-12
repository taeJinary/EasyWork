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
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
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
        setError('프로젝트를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    }
    void fetchProjects();
  }, [page, searchQuery, roleFilter]);

  return (
    <div>
      <PageHeader
        title="프로젝트"
        description="참여 중이거나 소유한 프로젝트를 한눈에 확인하세요."
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
            새 프로젝트
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
          <option value="">역할: 전체</option>
          <option value="OWNER">역할: 소유자</option>
          <option value="MEMBER">역할: 멤버</option>
        </select>
        <select
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="updated">정렬: 최근 수정</option>
          <option value="name">정렬: 이름</option>
          <option value="created">정렬: 생성일</option>
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
          {searchQuery || roleFilter
            ? '검색 조건에 맞는 프로젝트가 없습니다.'
            : '아직 프로젝트가 없습니다. 첫 프로젝트를 만들어보세요.'}
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
                <span>멤버 {project.memberCount}명</span>
                <span>미완료 작업 {project.openTaskCount}개</span>
                <span>최근 수정 {formatTimeAgo(project.updatedAt)}</span>
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
            이전
          </button>
          <span className="text-[var(--color-text-muted)]">페이지 {page + 1} / {totalPages}</span>
          <button
            onClick={() => setPage((currentPage) => Math.min(totalPages - 1, currentPage + 1))}
            disabled={page >= totalPages - 1}
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
              bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--color-text-secondary)]
              hover:bg-[var(--color-surface-muted)] disabled:cursor-not-allowed disabled:opacity-50
            "
          >
            다음
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}
