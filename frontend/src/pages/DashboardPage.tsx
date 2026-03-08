import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '@/components/PageHeader';
import apiClient from '@/api/client';
import type { ApiResponse, DashboardProjectsResponse, DashboardProjectSummary } from '@/types';

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

function ProjectRow({ project, onOpen }: { project: DashboardProjectSummary; onOpen: (projectId: number) => void }) {
  const openTaskCount = Math.max(project.taskCount - project.doneTaskCount, 0);

  return (
    <button
      type="button"
      onClick={() => onOpen(project.projectId)}
      className="w-full border-none bg-transparent px-[var(--spacing-base)] py-[var(--spacing-md)] text-left hover:bg-[var(--color-surface-muted)]"
    >
      <div className="flex items-start justify-between gap-[var(--spacing-base)]">
        <div className="min-w-0">
          <div className="flex items-center gap-[var(--spacing-sm)]">
            <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">{project.name}</span>
            <span className="rounded-full bg-[var(--color-surface-muted)] px-[8px] py-[2px] text-[11px] font-medium text-[var(--color-text-secondary)]">
              {project.role}
            </span>
          </div>
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
    </button>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<DashboardProjectsResponse | null>(null);
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

    fetchDashboard();

    return () => {
      isMounted = false;
    };
  }, []);

  const totalProjects = dashboard?.myProjects.length ?? 0;
  const totalTasks = useMemo(
    () => dashboard?.myProjects.reduce((sum, project) => sum + project.taskCount, 0) ?? 0,
    [dashboard]
  );

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
        <div className="mt-[var(--spacing-base)] overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {dashboard.myProjects.map((project, index) => (
            <div
              key={project.projectId}
              className={index < dashboard.myProjects.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
            >
              <ProjectRow project={project} onOpen={(projectId) => navigate(`/projects/${projectId}/board`)} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
