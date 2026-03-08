import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  DashboardProjectStatsResponse,
  DashboardProjectsResponse,
  DashboardProjectSummary,
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

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-base)]">
      <div className="text-[var(--text-xs)] uppercase tracking-wide text-[var(--color-text-muted)]">{label}</div>
      <div className="mt-[var(--spacing-xs)] text-[24px] font-bold text-[var(--color-text-primary)]">{value}</div>
    </div>
  );
}

function ProjectRow({
  project,
  onOpen,
  onInspect,
}: {
  project: DashboardProjectSummary;
  onOpen: (projectId: number) => void;
  onInspect: (projectId: number) => void;
}) {
  const openTaskCount = Math.max(project.taskCount - project.doneTaskCount, 0);

  return (
    <div className="px-[var(--spacing-base)] py-[var(--spacing-md)]">
      <div className="flex items-start justify-between gap-[var(--spacing-base)]">
        <div className="min-w-0">
          <button
            type="button"
            onClick={() => onOpen(project.projectId)}
            className="flex items-center gap-[var(--spacing-sm)] border-none bg-transparent p-0 text-left"
          >
            <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">{project.name}</span>
            <span className="rounded-full bg-[var(--color-surface-muted)] px-[8px] py-[2px] text-[11px] font-medium text-[var(--color-text-secondary)]">
              {project.role}
            </span>
          </button>
          <div className="mt-[var(--spacing-xs)] flex flex-wrap items-center gap-[var(--spacing-base)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
            <span>{project.memberCount} members</span>
            <span>{openTaskCount} open tasks</span>
            <span>{project.progressRate}% complete</span>
          </div>
        </div>
        <div className="shrink-0 text-[var(--text-xs)] text-[var(--color-text-muted)]">
          Updated {formatTimeAgo(project.updatedAt)}
        </div>
      </div>
      <div className="mt-[var(--spacing-sm)] flex justify-end">
        <button
          type="button"
          onClick={() => onInspect(project.projectId)}
          className="h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
        >
          View Stats
        </button>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<DashboardProjectsResponse | null>(null);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [projectStats, setProjectStats] = useState<DashboardProjectStatsResponse | null>(null);
  const [projectStatsLoading, setProjectStatsLoading] = useState(false);
  const [projectStatsError, setProjectStatsError] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function fetchDashboard() {
      try {
        setLoading(true);
        setError('');
        const response = await apiClient.get<ApiResponse<DashboardProjectsResponse>>('/dashboard/projects');
        if (isMounted) {
          setDashboard(response.data.data);
          setSelectedProjectId((current) => current ?? response.data.data.myProjects[0]?.projectId ?? null);
        }
      } catch {
        if (isMounted) {
          setError('Failed to load dashboard data.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    void fetchDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedProjectId) {
      setProjectStats(null);
      setProjectStatsError('');
      return;
    }

    let isMounted = true;

    async function fetchProjectStats() {
      try {
        setProjectStatsLoading(true);
        setProjectStatsError('');
        const response = await apiClient.get<ApiResponse<DashboardProjectStatsResponse>>(
          `/projects/${selectedProjectId}/dashboard`
        );
        if (isMounted) {
          setProjectStats(response.data.data);
        }
      } catch {
        if (isMounted) {
          setProjectStats(null);
          setProjectStatsError('Failed to load project stats.');
        }
      } finally {
        if (isMounted) {
          setProjectStatsLoading(false);
        }
      }
    }

    void fetchProjectStats();

    return () => {
      isMounted = false;
    };
  }, [selectedProjectId]);

  const totalProjects = dashboard?.myProjects.length ?? 0;
  const totalTasks = useMemo(
    () => dashboard?.myProjects.reduce((sum, project) => sum + project.taskCount, 0) ?? 0,
    [dashboard]
  );
  const selectedProjectName = dashboard?.myProjects.find((project) => project.projectId === selectedProjectId)?.name;

  return (
    <div>
      <PageHeader title="Dashboard" description="Track active projects and pending invitations in one place." />

      <div className="mt-[var(--spacing-base)] grid gap-[var(--spacing-base)] md:grid-cols-3">
        <StatCard label="Pending invitations" value={dashboard?.pendingInvitationCount ?? 0} />
        <StatCard label="Active projects" value={totalProjects} />
        <StatCard label="Tracked tasks" value={totalTasks} />
      </div>

      {error && (
        <div className="mt-[var(--spacing-base)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-danger)]">
          {error}
        </div>
      )}

      {loading && (
        <div className="mt-[var(--spacing-base)] space-y-[var(--spacing-sm)]">
          {[1, 2, 3].map((item) => (
            <div key={item} className="h-[72px] animate-pulse rounded-[var(--radius-md)] bg-[var(--color-surface-muted)]" />
          ))}
        </div>
      )}

      {!loading && !error && dashboard && dashboard.myProjects.length === 0 && (
        <div className="mt-[var(--spacing-base)] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
          No active projects yet.
        </div>
      )}

      {!loading && !error && dashboard && dashboard.myProjects.length > 0 && (
        <div className="mt-[var(--spacing-base)] grid gap-[var(--spacing-base)] lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
            {dashboard.myProjects.map((project, index) => (
              <div
                key={project.projectId}
                className={index < dashboard.myProjects.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
              >
                <ProjectRow
                  project={project}
                  onOpen={(projectId) => navigate(`/projects/${projectId}/board`)}
                  onInspect={(projectId) => setSelectedProjectId(projectId)}
                />
              </div>
            ))}
          </div>

          <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-base)]">
            <div className="mb-[var(--spacing-base)] flex items-center justify-between gap-[var(--spacing-sm)]">
              <div>
                <div className="text-[var(--text-xs)] uppercase tracking-wide text-[var(--color-text-muted)]">Project Stats</div>
                <div className="mt-[var(--spacing-xs)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {selectedProjectName ?? 'Select a project'}
                </div>
              </div>
              {selectedProjectId && (
                <button
                  type="button"
                  onClick={() => navigate(`/projects/${selectedProjectId}/board`)}
                  className="h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
                >
                  Open Board
                </button>
              )}
            </div>

            {projectStatsError && (
              <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                {projectStatsError}
              </div>
            )}

            {projectStatsLoading && (
              <div className="space-y-[var(--spacing-sm)]">
                {[1, 2, 3].map((item) => (
                  <div key={item} className="h-[48px] animate-pulse rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)]" />
                ))}
              </div>
            )}

            {!projectStatsLoading && !projectStatsError && !projectStats && (
              <div className="text-[var(--text-sm)] text-[var(--color-text-muted)]">
                Select a project to inspect workload and due-date risk.
              </div>
            )}

            {!projectStatsLoading && !projectStatsError && projectStats && (
              <div className="grid gap-[var(--spacing-sm)] sm:grid-cols-2">
                <StatCard label="Members" value={projectStats.memberCount} />
                <StatCard label="Tasks" value={projectStats.taskCount} />
                <StatCard label="TODO" value={projectStats.todoCount} />
                <StatCard label="In Progress" value={projectStats.inProgressCount} />
                <StatCard label="Done" value={projectStats.doneCount} />
                <StatCard label="Completion" value={`${projectStats.completionRate}%`} />
                <StatCard label="Overdue" value={projectStats.overdueCount} />
                <StatCard label="Due Soon" value={projectStats.dueSoonCount} />
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
