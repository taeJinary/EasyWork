import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Bell, LogOut, Menu, Plus, Search, Settings, User } from 'lucide-react';
import apiClient from '@/api/client';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import type {
  ApiResponse,
  NotificationUnreadCount,
  ProjectListResponse,
  WorkspaceListItemResponse,
  WorkspaceListResponse,
} from '@/types';

type SearchPreviewProject = {
  id: number;
  name: string;
  description?: string;
};

type SearchPreviewWorkspace = {
  id: number;
  name: string;
  description?: string;
};

function normalizeWorkspace(workspace: WorkspaceListItemResponse): SearchPreviewWorkspace {
  return {
    id: workspace.workspaceId,
    name: workspace.name,
    description: workspace.description,
  };
}

function GlobalSearchForm({
  initialQuery,
}: {
  initialQuery: string;
}) {
  const navigate = useNavigate();
  const searchRef = useRef<HTMLFormElement>(null);
  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [previewProjects, setPreviewProjects] = useState<SearchPreviewProject[]>([]);
  const [previewWorkspaces, setPreviewWorkspaces] = useState<SearchPreviewWorkspace[]>([]);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setPreviewOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const normalizedQuery = searchQuery.trim();

    async function fetchPreview() {
      if (normalizedQuery.length < 2) {
        setPreviewProjects([]);
        setPreviewWorkspaces([]);
        setLoadingPreview(false);
        setPreviewOpen(false);
        return;
      }

      try {
        setLoadingPreview(true);

        const [projectResponse, workspaceResponse] = await Promise.all([
          apiClient.get<ApiResponse<ProjectListResponse>>('/projects', {
            params: { page: 0, size: 5, keyword: normalizedQuery },
          }),
          apiClient.get<ApiResponse<WorkspaceListResponse>>('/workspaces'),
        ]);

        if (cancelled) {
          return;
        }

        const lowerQuery = normalizedQuery.toLowerCase();
        const filteredWorkspaces = workspaceResponse.data.data.content
          .map(normalizeWorkspace)
          .filter((workspace) => {
            const workspaceName = workspace.name.toLowerCase();
            const workspaceDescription = workspace.description?.toLowerCase() ?? '';
            return workspaceName.includes(lowerQuery) || workspaceDescription.includes(lowerQuery);
          })
          .slice(0, 5);

        setPreviewProjects(
          projectResponse.data.data.content.map((project) => ({
            id: project.projectId,
            name: project.name,
            description: project.description,
          }))
        );
        setPreviewWorkspaces(filteredWorkspaces);
        setPreviewOpen(true);
      } catch {
        if (!cancelled) {
          setPreviewProjects([]);
          setPreviewWorkspaces([]);
          setPreviewOpen(true);
        }
      } finally {
        if (!cancelled) {
          setLoadingPreview(false);
        }
      }
    }

    void fetchPreview();

    return () => {
      cancelled = true;
    };
  }, [searchQuery]);

  const handleSearchSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedQuery = searchQuery.trim();
    if (!normalizedQuery) {
      return;
    }

    setPreviewOpen(false);
    navigate(`/search?q=${encodeURIComponent(normalizedQuery)}`);
  };

  const hasPreviewResults = previewProjects.length > 0 || previewWorkspaces.length > 0;
  const shouldShowPreview = previewOpen && searchQuery.trim().length >= 2;

  return (
    <form
      ref={searchRef}
      onSubmit={handleSearchSubmit}
      className="relative hidden max-w-[480px] flex-1 md:flex"
    >
      <Search
        size={16}
        className="absolute left-[var(--spacing-sm)] top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
      />
      <input
        name="q"
        type="text"
        placeholder="검색..."
        value={searchQuery}
        onChange={(event) => setSearchQuery(event.target.value)}
        onFocus={() => {
          if (searchQuery.trim().length >= 2) {
            setPreviewOpen(true);
          }
        }}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            setPreviewOpen(false);
          }
        }}
        className="
          h-[32px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
          bg-[var(--color-surface-muted)] pl-[30px] pr-[var(--spacing-sm)] text-[var(--text-sm)]
          text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)]
          focus:bg-[var(--color-surface)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
        "
      />

      {shouldShowPreview && (
        <div
          className="
            absolute left-0 right-0 top-[calc(100%+8px)] z-50 overflow-hidden rounded-[var(--radius-md)]
            border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-sm)]
          "
        >
          {loadingPreview && (
            <div className="px-[var(--spacing-base)] py-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
              검색 중...
            </div>
          )}

          {!loadingPreview && !hasPreviewResults && (
            <div className="px-[var(--spacing-base)] py-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
              검색 결과가 없습니다.
            </div>
          )}

          {!loadingPreview && hasPreviewResults && (
            <div className="max-h-[320px] overflow-y-auto">
              {previewProjects.length > 0 && (
                <div>
                  <div className="border-b border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-xs)] text-[11px] font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
                    프로젝트
                  </div>
                  <ul className="m-0 list-none p-0">
                    {previewProjects.map((project) => (
                      <li key={project.id} className="border-b border-[var(--color-border)] last:border-b-0">
                        <Link
                          to={`/projects/${project.id}/board`}
                          aria-label={project.name}
                          onClick={() => setPreviewOpen(false)}
                          className="block px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]"
                        >
                          <div className="font-medium">{project.name}</div>
                          {project.description && (
                            <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                              {project.description}
                            </div>
                          )}
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {previewWorkspaces.length > 0 && (
                <div>
                  <div className="border-b border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-xs)] text-[11px] font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
                    작업공간
                  </div>
                  <ul className="m-0 list-none p-0">
                    {previewWorkspaces.map((workspace) => (
                      <li key={workspace.id} className="border-b border-[var(--color-border)] last:border-b-0">
                        <Link
                          to={`/workspaces/${workspace.id}`}
                          aria-label={workspace.name}
                          onClick={() => setPreviewOpen(false)}
                          className="block px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]"
                        >
                          <div className="font-medium">{workspace.name}</div>
                          {workspace.description && (
                            <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                              {workspace.description}
                            </div>
                          )}
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="border-t border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-xs)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                Enter를 누르면 전체 검색 결과로 이동합니다.
              </div>
            </div>
          )}
        </div>
      )}
    </form>
  );
}

export default function GlobalHeader() {
  const { user, logout } = useAuthStore();
  const { toggleMobileSidebar } = useUiStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [profileOpen, setProfileOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function fetchUnreadCount() {
      try {
        const response = await apiClient.get<ApiResponse<NotificationUnreadCount>>('/notifications/unread-count');
        if (isMounted) {
          setUnreadCount(response.data.data.unreadCount);
        }
      } catch {
        if (isMounted) {
          setUnreadCount(0);
        }
      }
    }

    if (user) {
      fetchUnreadCount();
    }

    return () => {
      isMounted = false;
    };
  }, [user]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const displayName = user?.nickname?.trim() || '사용자';
  const avatarInitial = displayName.charAt(0).toUpperCase() || 'U';

  const handleCreateWorkspace = () => {
    navigate('/workspaces?create=workspace');
  };

  return (
    <header
      className="
        fixed left-0 right-0 top-0 z-[60] flex h-[56px] items-center gap-[var(--spacing-base)]
        border-b border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-base)]
      "
    >
      <button
        onClick={toggleMobileSidebar}
        className="
          flex h-[32px] w-[32px] shrink-0 items-center justify-center rounded-[var(--radius-sm)]
          border-none bg-transparent text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] md:hidden
        "
        aria-label="메뉴 열기"
      >
        <Menu size={20} />
      </button>

      <Link
        to="/dashboard"
        className="shrink-0 text-[var(--text-base)] font-bold text-[var(--color-text-primary)] no-underline hover:no-underline"
      >
        EasyWork
      </Link>

      <GlobalSearchForm
        key={`${location.pathname}${location.search}`}
        initialQuery={location.pathname === '/search'
          ? new URLSearchParams(location.search).get('q')?.trim() ?? ''
          : ''}
      />

      <div className="ml-auto flex items-center gap-[var(--spacing-sm)]">
        <button
          onClick={handleCreateWorkspace}
          className="
            flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border-none
            bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
            hover:bg-[var(--color-primary-hover)]
          "
        >
          <Plus size={16} />
          <span>새로 만들기</span>
        </button>

        <Link
          to="/notifications"
          aria-label="Notifications"
          className="
            relative flex h-[32px] w-[32px] items-center justify-center rounded-[var(--radius-sm)]
            text-[var(--color-text-secondary)] no-underline hover:bg-[var(--color-surface-muted)]
          "
        >
          <Bell size={18} />
          {unreadCount > 0 && (
            <span
              className="
                absolute -right-[4px] -top-[4px] min-w-[18px] rounded-full
                bg-[var(--color-danger)] px-[4px] py-[1px] text-center
                text-[10px] font-bold leading-none text-white
              "
            >
              {unreadCount}
            </span>
          )}
        </Link>

        <div ref={profileRef} className="relative">
          <button
            onClick={() => setProfileOpen((prev) => !prev)}
            className="
              flex h-[32px] w-[32px] items-center justify-center rounded-full border-none
              bg-[var(--color-primary)] text-[var(--text-xs)] font-semibold text-white hover:opacity-90
            "
          >
            {avatarInitial}
          </button>

          {profileOpen && (
            <div
              className="
                absolute right-0 top-[40px] z-50 w-[200px] rounded-[var(--radius-md)]
                border border-[var(--color-border)] bg-[var(--color-surface)] py-[var(--spacing-xs)] shadow-[var(--shadow-sm)]
              "
            >
              <div className="border-b border-[var(--color-border)] px-[var(--spacing-base)] py-[var(--spacing-sm)]">
                <div className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {displayName}
                </div>
                <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                  {user?.email || ''}
                </div>
              </div>
              <Link
                to="/settings/profile"
                onClick={() => setProfileOpen(false)}
                className="
                  flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)]
                  text-[var(--text-sm)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]
                "
              >
                <User size={14} />
                프로필
              </Link>
              <Link
                to="/settings/account"
                onClick={() => setProfileOpen(false)}
                className="
                  flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-base)] py-[var(--spacing-sm)]
                  text-[var(--text-sm)] text-[var(--color-text-primary)] no-underline hover:bg-[var(--color-surface-muted)]
                "
              >
                <Settings size={14} />
                설정
              </Link>
              <div className="mt-[var(--spacing-xs)] border-t border-[var(--color-border)]">
                <button
                  onClick={handleLogout}
                  className="
                    flex w-full items-center gap-[var(--spacing-sm)] border-none bg-transparent
                    px-[var(--spacing-base)] py-[var(--spacing-sm)] text-left text-[var(--text-sm)]
                    text-[var(--color-danger)] hover:bg-[var(--color-surface-muted)]
                  "
                >
                  <LogOut size={14} />
                  로그아웃
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
