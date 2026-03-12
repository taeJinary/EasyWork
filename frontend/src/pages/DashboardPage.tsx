import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
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
            <span>멤버 {project.memberCount}명</span>
            <span>미완료 작업 {openTaskCount}개</span>
            <span>완료율 {project.progressRate}%</span>
          </div>
        </div>
        <div className="shrink-0 text-[var(--text-xs)] text-[var(--color-text-muted)]">
          업데이트 {formatTimeAgo(project.updatedAt)}
        </div>
      </div>
      <div className="mt-[var(--spacing-sm)] flex justify-end">
        <button
          type="button"
          onClick={() => onInspect(project.projectId)}
          className="h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
        >
          통계 보기
        </button>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const mountedRef = useRef(true);
  const dashboardRequestIdRef = useRef(0);
  const projectStatsRequestIdRef = useRef(0);
  const [dashboard, setDashboard] = useState<DashboardProjectsResponse | null>(null);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [projectStats, setProjectStats] = useState<DashboardProjectStatsResponse | null>(null);
  const [projectStatsLoading, setProjectStatsLoading] = useState(false);
  const [projectStatsError, setProjectStatsError] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    mountedRef.current = true;

    return () => {
      mountedRef.current = false;
    };
  }, []);

  const fetchDashboard = useCallback(
    async () => {
      const requestId = ++dashboardRequestIdRef.current;

      try {
        setLoading(true);
        setError('');
        const response = await apiClient.get<ApiResponse<DashboardProjectsResponse>>('/dashboard/projects');
        if (mountedRef.current && requestId === dashboardRequestIdRef.current) {
          setDashboard(response.data.data);
          setSelectedProjectId((current) => current ?? response.data.data.myProjects[0]?.projectId ?? null);
        }
      } catch {
        if (mountedRef.current && requestId === dashboardRequestIdRef.current) {
          setDashboard(null);
          setSelectedProjectId(null);
          setError('대시보드 데이터를 불러오지 못했습니다.');
        }
      } finally {
        if (mountedRef.current && requestId === dashboardRequestIdRef.current) {
          setLoading(false);
        }
      }
    },
    []
  );

  useEffect(() => {
    void fetchDashboard();
  }, [fetchDashboard]);

  const fetchProjectStats = useCallback(
    async (projectId: number) => {
      const requestId = ++projectStatsRequestIdRef.current;

      try {
        setProjectStatsLoading(true);
        setProjectStatsError('');
        const response = await apiClient.get<ApiResponse<DashboardProjectStatsResponse>>(`/projects/${projectId}/dashboard`);
        if (mountedRef.current && requestId === projectStatsRequestIdRef.current) {
          setProjectStats(response.data.data);
        }
      } catch {
        if (mountedRef.current && requestId === projectStatsRequestIdRef.current) {
          setProjectStats(null);
          setProjectStatsError('프로젝트 통계를 불러오지 못했습니다.');
        }
      } finally {
        if (mountedRef.current && requestId === projectStatsRequestIdRef.current) {
          setProjectStatsLoading(false);
        }
      }
    },
    []
  );

  useEffect(() => {
    if (!selectedProjectId) {
      projectStatsRequestIdRef.current += 1;
      setProjectStats(null);
      setProjectStatsError('');
      return;
    }

    void fetchProjectStats(selectedProjectId);
  }, [fetchProjectStats, selectedProjectId]);

  const totalProjects = dashboard?.myProjects.length ?? 0;
  const totalTasks = useMemo(
    () => dashboard?.myProjects.reduce((sum, project) => sum + project.taskCount, 0) ?? 0,
    [dashboard]
  );
  const selectedProjectName = dashboard?.myProjects.find((project) => project.projectId === selectedProjectId)?.name;

  return (
    <div>
      <PageHeader title="대시보드" description="참여 중인 프로젝트와 초대 현황을 한곳에서 확인하세요." />

      <div className="mt-[var(--spacing-base)] grid gap-[var(--spacing-base)] md:grid-cols-3">
        <StatCard label="대기 중 초대" value={dashboard?.pendingInvitationCount ?? 0} />
        <StatCard label="진행 중 프로젝트" value={totalProjects} />
        <StatCard label="추적 중 작업" value={totalTasks} />
      </div>

      {error && (
        <div className="mt-[var(--spacing-base)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <div>{error}</div>
          <button
            type="button"
            onClick={() => void fetchDashboard()}
            className="mt-[var(--spacing-sm)] h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-transparent px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-danger)] hover:bg-white/40"
          >
            다시 시도
          </button>
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
          <div>아직 참여 중인 프로젝트가 없습니다.</div>
          <div className="mt-[var(--spacing-sm)]">프로젝트를 만들거나 초대를 수락하면 진행 현황을 확인할 수 있습니다.</div>
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
                <div className="text-[var(--text-xs)] uppercase tracking-wide text-[var(--color-text-muted)]">프로젝트 통계</div>
                <div className="mt-[var(--spacing-xs)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                  {selectedProjectName ?? '프로젝트를 선택하세요'}
                </div>
              </div>
              {selectedProjectId && (
                <button
                  type="button"
                  onClick={() => navigate(`/projects/${selectedProjectId}/board`)}
                  className="h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
                >
                  보드 열기
                </button>
              )}
            </div>

            {projectStatsError && (
              <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                <div>{projectStatsError}</div>
                {selectedProjectId && (
                  <button
                    type="button"
                    onClick={() => void fetchProjectStats(selectedProjectId)}
                    className="mt-[var(--spacing-sm)] h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-transparent px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-danger)] hover:bg-white/40"
                  >
                    다시 시도
                  </button>
                )}
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
                프로젝트를 선택하면 작업량과 마감 위험을 확인할 수 있습니다.
              </div>
            )}

            {!projectStatsLoading && !projectStatsError && projectStats && (
              <div className="grid gap-[var(--spacing-sm)] sm:grid-cols-2">
                <StatCard label="멤버" value={projectStats.memberCount} />
                <StatCard label="작업" value={projectStats.taskCount} />
                <StatCard label="할 일" value={projectStats.todoCount} />
                <StatCard label="진행 중" value={projectStats.inProgressCount} />
                <StatCard label="완료" value={projectStats.doneCount} />
                <StatCard label="완료율" value={`${projectStats.doneCount}/${projectStats.taskCount}`} />
                <StatCard label="기한 초과" value={projectStats.overdueCount} />
                <StatCard label="마감 임박" value={projectStats.dueSoonCount} />
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

