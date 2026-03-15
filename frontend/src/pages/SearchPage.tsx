import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FolderKanban, Search, Users } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  ProjectListResponse,
  WorkspaceListItemResponse,
  WorkspaceListResponse,
} from '@/types';

type SearchProject = {
  id: number;
  name: string;
  description?: string;
  role: 'OWNER' | 'MEMBER';
};

type SearchWorkspace = {
  id: number;
  name: string;
  description?: string;
  myRole: 'OWNER' | 'MEMBER';
  memberCount: number;
};

function normalizeWorkspace(workspace: WorkspaceListItemResponse): SearchWorkspace {
  return {
    id: workspace.workspaceId,
    name: workspace.name,
    description: workspace.description,
    myRole: workspace.myRole,
    memberCount: workspace.memberCount,
  };
}

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const query = useMemo(() => searchParams.get('q')?.trim() ?? '', [searchParams]);
  const [projects, setProjects] = useState<SearchProject[]>([]);
  const [workspaces, setWorkspaces] = useState<SearchWorkspace[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function fetchResults() {
      if (!query) {
        setProjects([]);
        setWorkspaces([]);
        setError('');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError('');

        const [projectResponse, workspaceResponse] = await Promise.all([
          apiClient.get<ApiResponse<ProjectListResponse>>('/projects', {
            params: { page: 0, size: 20, keyword: query },
          }),
          apiClient.get<ApiResponse<WorkspaceListResponse>>('/workspaces'),
        ]);

        if (cancelled) {
          return;
        }

        const normalizedQuery = query.toLowerCase();
        const filteredWorkspaces = workspaceResponse.data.data.content
          .map(normalizeWorkspace)
          .filter((workspace) => {
            const workspaceName = workspace.name.toLowerCase();
            const workspaceDescription = workspace.description?.toLowerCase() ?? '';
            return workspaceName.includes(normalizedQuery) || workspaceDescription.includes(normalizedQuery);
          });

        setProjects(
          projectResponse.data.data.content.map((project) => ({
            id: project.projectId,
            name: project.name,
            description: project.description,
            role: project.role,
          }))
        );
        setWorkspaces(filteredWorkspaces);
      } catch {
        if (!cancelled) {
          setProjects([]);
          setWorkspaces([]);
          setError('검색 결과를 불러오지 못했습니다.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void fetchResults();

    return () => {
      cancelled = true;
    };
  }, [query]);

  const hasResults = projects.length > 0 || workspaces.length > 0;

  return (
    <div>
      <PageHeader
        title="검색"
        description={query ? `"${query}" 검색 결과입니다.` : '프로젝트와 작업공간을 검색하세요.'}
      />

      {!query && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
          상단 검색창에 키워드를 입력하면 프로젝트와 작업공간을 찾을 수 있습니다.
        </div>
      )}

      {error && (
        <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-danger)]">
          {error}
        </div>
      )}

      {loading && (
        <div className="space-y-[var(--spacing-base)]">
          {[1, 2].map((item) => (
            <div
              key={item}
              className="h-[96px] animate-pulse rounded-[var(--radius-md)] bg-[var(--color-surface-muted)]"
            />
          ))}
        </div>
      )}

      {!loading && query && !error && !hasResults && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
          검색 결과가 없습니다.
        </div>
      )}

      {!loading && query && !error && hasResults && (
        <div className="space-y-[var(--spacing-base)]">
          {projects.length > 0 && (
            <section className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
              <div className="border-b border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                프로젝트
              </div>
              <ul className="m-0 list-none p-0">
                {projects.map((project) => (
                  <li key={project.id} className="border-b border-[var(--color-border)] last:border-b-0">
                    <Link
                      to={`/projects/${project.id}/board`}
                      aria-label={project.name}
                      className="flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-base)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]"
                    >
                      <FolderKanban size={16} className="shrink-0 text-[var(--color-text-muted)]" />
                      <div className="min-w-0">
                        <div className="font-medium">{project.name}</div>
                        {project.description && (
                          <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                            {project.description}
                          </div>
                        )}
                      </div>
                      <span className="ml-auto shrink-0 text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        {project.role}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {workspaces.length > 0 && (
            <section className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
              <div className="border-b border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                작업공간
              </div>
              <ul className="m-0 list-none p-0">
                {workspaces.map((workspace) => (
                  <li key={workspace.id} className="border-b border-[var(--color-border)] last:border-b-0">
                    <Link
                      to={`/workspaces/${workspace.id}`}
                      aria-label={workspace.name}
                      className="flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-base)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]"
                    >
                      <Users size={16} className="shrink-0 text-[var(--color-text-muted)]" />
                      <div className="min-w-0">
                        <div className="font-medium">{workspace.name}</div>
                        {workspace.description && (
                          <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                            {workspace.description}
                          </div>
                        )}
                      </div>
                      <span className="ml-auto shrink-0 text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        멤버 {workspace.memberCount}명
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      )}

      {!query && (
        <div className="mt-[var(--spacing-base)] flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
          <Search size={16} />
          프로젝트 이름, 작업공간 이름, 설명을 기준으로 검색합니다.
        </div>
      )}
    </div>
  );
}
