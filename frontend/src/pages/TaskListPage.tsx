import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertCircle,
  Calendar,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Circle,
  Loader,
  MessageSquare,
  Plus,
  UserPlus,
} from 'lucide-react';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import TaskDetailDrawer from '@/components/TaskDetailDrawer';
import apiClient from '@/api/client';
import { toProjectDetail } from '@/utils/projectMappers';
import type {
  ApiResponse,
  ProjectDetail,
  ProjectDetailResponse,
  TaskListItem,
  TaskListResponse,
  TaskStatus,
} from '@/types';

type TabType = 'board' | 'list' | 'members' | 'settings';

const statusIcons: Record<TaskStatus, typeof Circle> = {
  TODO: Circle,
  IN_PROGRESS: Loader,
  DONE: CheckCircle2,
};

const statusColors: Record<TaskStatus, string> = {
  TODO: 'var(--color-text-muted)',
  IN_PROGRESS: 'var(--color-warning)',
  DONE: 'var(--color-success)',
};

const priorityVariant: Record<string, 'danger' | 'warning' | 'primary' | 'muted'> = {
  URGENT: 'danger',
  HIGH: 'warning',
  MEDIUM: 'primary',
  LOW: 'muted',
};

function formatDate(dateStr: string): string {
  const [, month, day] = dateStr.split('-');
  const monthNames = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${monthNames[parseInt(month, 10)]} ${parseInt(day, 10)}`;
}

export default function TaskListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [tasks, setTasks] = useState<TaskListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortBy, setSortBy] = useState('updatedAt');
  const [direction, setDirection] = useState('DESC');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('list');
  const [refetchTrigger, setRefetchTrigger] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!projectId) {
      return;
    }

    let cancelled = false;

    async function fetchList() {
      try {
        setLoading(true);
        setError(null);
        const params: Record<string, string | number> = { page, size: 20, sortBy, direction };
        if (statusFilter) params.status = statusFilter;
        if (searchQuery) params.keyword = searchQuery;

        const [projectResponse, tasksResponse] = await Promise.all([
          apiClient.get<ApiResponse<ProjectDetailResponse>>(`/projects/${projectId}`),
          apiClient.get<ApiResponse<TaskListResponse>>(`/projects/${projectId}/tasks`, { params }),
        ]);

        if (cancelled) {
          return;
        }

        setProject(toProjectDetail(projectResponse.data.data));
        setTasks(tasksResponse.data.data.content);
        setTotalPages(tasksResponse.data.data.totalPages);
      } catch (caughtError) {
        if (cancelled) {
          return;
        }

        setError('Failed to load tasks.');
        console.error('Failed to fetch task list:', caughtError);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void fetchList();

    return () => {
      cancelled = true;
    };
  }, [projectId, page, statusFilter, searchQuery, sortBy, direction, refetchTrigger]);

  const refreshList = () => {
    setRefetchTrigger((value) => value + 1);
  };

  const handleTabClick = (tab: TabType) => {
    if (tab === 'board') {
      navigate(`/projects/${projectId}/board`);
    } else if (tab === 'members') {
      navigate(`/projects/${projectId}/members`);
    } else if (tab === 'settings') {
      navigate(`/projects/${projectId}/settings`);
    } else {
      setActiveTab(tab);
    }
  };

  const tabs: { key: TabType; label: string }[] = [
    { key: 'board', label: 'Board' },
    { key: 'list', label: 'List' },
    { key: 'members', label: 'Members' },
    { key: 'settings', label: 'Settings' },
  ];

  return (
    <div>
      <div className="flex items-start justify-between border-b border-[var(--color-border)] pb-[var(--spacing-base)]">
        <div>
          <div className="mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
            <span className="cursor-pointer hover:text-[var(--color-primary)]" onClick={() => navigate('/workspaces')}>
              Workspace
            </span>
            <span>/</span>
            <span className="font-medium text-[var(--color-text-primary)]">{project?.name}</span>
          </div>
          <h1 className="m-0 text-[var(--text-lg)] font-bold text-[var(--color-text-primary)]">
            {project?.name}
          </h1>
          {project?.description && (
            <p className="m-0 mt-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
              {project.description}
            </p>
          )}
        </div>
        <div className="flex items-center gap-[var(--spacing-sm)]">
          <button
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
              bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-primary)]
              hover:bg-[var(--color-surface-muted)]
            "
          >
            <UserPlus size={14} />
            Invite
          </button>
          <button
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border-none
              bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
              hover:bg-[var(--color-primary-hover)]
            "
          >
            <Plus size={14} />
            New Task
          </button>
        </div>
      </div>

      <div className="mt-[var(--spacing-base)] flex border-b border-[var(--color-border)]">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => handleTabClick(tab.key)}
            className={`
              border-b-2 bg-transparent px-[var(--spacing-base)] py-[var(--spacing-sm)] text-[var(--text-sm)]
              ${
                activeTab === tab.key
                  ? 'border-[var(--color-primary)] font-semibold text-[var(--color-text-primary)]'
                  : 'border-transparent text-[var(--color-text-secondary)] hover:border-[var(--color-border)] hover:text-[var(--color-text-primary)]'
              }
            `}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <div
          className="
            mt-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] rounded-[var(--radius-sm)]
            border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)]
            text-[var(--text-sm)] text-[var(--color-danger)]
          "
        >
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      <FilterBar
        searchPlaceholder="Search tasks..."
        searchValue={searchQuery}
        onSearchChange={(value) => {
          setSearchQuery(value);
          setPage(0);
        }}
      >
        <select
          value={statusFilter}
          onChange={(event) => {
            setStatusFilter(event.target.value);
            setPage(0);
          }}
          className="
            h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:border-[var(--color-primary)] focus:outline-none
          "
        >
          <option value="">Status: All</option>
          <option value="TODO">TODO</option>
          <option value="IN_PROGRESS">IN PROGRESS</option>
          <option value="DONE">DONE</option>
        </select>
        <select
          value={sortBy}
          onChange={(event) => {
            setSortBy(event.target.value);
            setPage(0);
          }}
          className="
            h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:border-[var(--color-primary)] focus:outline-none
          "
        >
          <option value="updatedAt">Sort: Updated</option>
          <option value="createdAt">Sort: Created</option>
          <option value="dueDate">Sort: Due Date</option>
        </select>
        <select
          value={direction}
          onChange={(event) => {
            setDirection(event.target.value);
            setPage(0);
          }}
          className="
            h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:border-[var(--color-primary)] focus:outline-none
          "
        >
          <option value="DESC">Direction: Desc</option>
          <option value="ASC">Direction: Asc</option>
        </select>
      </FilterBar>

      {loading && (
        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {[1, 2, 3, 4, 5].map((item) => (
            <div
              key={item}
              className="animate-pulse border-b border-[var(--color-border)] p-[var(--spacing-base)] last:border-b-0"
            >
              <div className="mb-2 h-4 w-3/4 rounded bg-[var(--color-surface-muted)]" />
              <div className="h-3 w-1/2 rounded bg-[var(--color-surface-muted)]" />
            </div>
          ))}
        </div>
      )}

      {!loading && tasks.length === 0 && (
        <div className="py-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
          {searchQuery || statusFilter ? 'No tasks matched your filter.' : 'No tasks yet. Create the first task.'}
        </div>
      )}

      {!loading && tasks.length > 0 && (
        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {tasks.map((task, index) => {
            const StatusIcon = statusIcons[task.status];
            return (
              <div
                key={task.taskId}
                onClick={() => setSelectedTaskId(task.taskId)}
                className={`
                  cursor-pointer px-[var(--spacing-base)] py-[var(--spacing-md)] transition-colors hover:bg-[var(--color-surface-muted)]
                  ${index < tasks.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                `}
              >
                <div className="flex items-center gap-[var(--spacing-sm)]">
                  <StatusIcon
                    size={16}
                    style={{ color: statusColors[task.status] }}
                    className={`shrink-0 ${task.status === 'IN_PROGRESS' ? 'animate-spin' : ''}`}
                  />
                  <span className="shrink-0 font-mono text-[var(--text-xs)] text-[var(--color-text-muted)]">
                    TASK-{task.taskId}
                  </span>
                  <span className="truncate text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                    {task.title}
                  </span>
                  <div className="ml-auto flex shrink-0 items-center gap-[var(--spacing-md)]">
                    {task.commentCount > 0 && (
                      <span className="flex items-center gap-[2px] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        <MessageSquare size={12} />
                        {task.commentCount}
                      </span>
                    )}
                  </div>
                </div>

                <div className="ml-[24px] mt-[var(--spacing-xs)] flex items-center gap-[var(--spacing-md)]">
                  <Badge variant={priorityVariant[task.priority] || 'muted'} size="sm">
                    {task.priority}
                  </Badge>

                  {task.assignee && (
                    <span className="flex items-center gap-[2px] text-[var(--text-xs)] text-[var(--color-text-secondary)]">
                      <div
                        className="
                          flex h-[14px] w-[14px] items-center justify-center rounded-full bg-[var(--color-primary)]
                          text-[8px] font-semibold text-white
                        "
                      >
                        {task.assignee.nickname.charAt(0).toUpperCase()}
                      </div>
                      {task.assignee.nickname}
                    </span>
                  )}

                  {task.dueDate && (
                    <span className="flex items-center gap-[2px] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                      <Calendar size={11} />
                      {formatDate(task.dueDate)}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="mt-[var(--spacing-base)] flex items-center justify-between text-[var(--text-sm)]">
          <button
            onClick={() => setPage((value) => Math.max(0, value - 1))}
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
          <span className="text-[var(--color-text-muted)]">
            Page {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
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

      {selectedTaskId && (
        <TaskDetailDrawer
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onStatusChange={refreshList}
        />
      )}
    </div>
  );
}
