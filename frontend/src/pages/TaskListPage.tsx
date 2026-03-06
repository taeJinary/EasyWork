import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, UserPlus, ChevronLeft, ChevronRight, Circle, Loader, CheckCircle2, MessageSquare, Calendar } from 'lucide-react';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import TaskDetailDrawer from '@/components/TaskDetailDrawer';
import apiClient from '@/api/client';
import type { ApiResponse, ProjectDetail, TaskListResponse, TaskListItem, TaskStatus } from '@/types';

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
  const d = new Date(dateStr);
  return `${d.toLocaleString('en', { month: 'short' })} ${d.getDate()}`;
}

export default function TaskListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [tasks, setTasks] = useState<TaskListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sortBy, setSortBy] = useState('updatedAt');
  const [direction, setDirection] = useState('DESC');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('list');

  const fetchData = useCallback(async () => {
    if (!projectId) return;
    try {
      setLoading(true);
      const params: Record<string, string | number> = { page, size: 20, sortBy, direction };
      if (statusFilter) params.status = statusFilter;
      if (searchQuery) params.keyword = searchQuery;

      const [projectRes, tasksRes] = await Promise.all([
        apiClient.get<ApiResponse<ProjectDetail>>(`/projects/${projectId}`),
        apiClient.get<ApiResponse<TaskListResponse>>(`/projects/${projectId}/tasks`, { params }),
      ]);
      setProject(projectRes.data.data);
      setTasks(tasksRes.data.data.content);
      setTotalPages(tasksRes.data.data.totalPages);
    } catch {
      // Error handling
    } finally {
      setLoading(false);
    }
  }, [projectId, page, statusFilter, searchQuery, sortBy, direction]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

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

  const handleStatusChange = async (taskId: number, newStatus: string) => {
    void taskId;
    void newStatus;
    await fetchData();
  };

  return (
    <div>
      {/* Header */}
      <div className="flex items-start justify-between pb-[var(--spacing-base)] border-b border-[var(--color-border)]">
        <div>
          <div className="flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)] mb-[var(--spacing-xs)]">
            <span
              className="cursor-pointer hover:text-[var(--color-primary)]"
              onClick={() => navigate('/workspaces')}
            >
              Workspace
            </span>
            <span>/</span>
            <span className="text-[var(--color-text-primary)] font-medium">{project?.name}</span>
          </div>
          <h1 className="text-[var(--text-lg)] font-bold text-[var(--color-text-primary)] m-0">
            {project?.name}
          </h1>
          {project?.description && (
            <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] m-0">
              {project.description}
            </p>
          )}
        </div>
        <div className="flex items-center gap-[var(--spacing-sm)]">
          <button className="
            flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
            border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            cursor-pointer hover:bg-[var(--color-surface-muted)]
          ">
            <UserPlus size={14} />
            Invite
          </button>
          <button className="
            flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
            bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
            text-[var(--text-sm)] font-medium border-none cursor-pointer
            hover:bg-[var(--color-primary-hover)]
          ">
            <Plus size={14} />
            New Task
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-[var(--color-border)] mt-[var(--spacing-base)]">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => handleTabClick(tab.key)}
            className={`
              px-[var(--spacing-base)] py-[var(--spacing-sm)]
              text-[var(--text-sm)] border-b-2 bg-transparent cursor-pointer
              ${activeTab === tab.key
                ? 'border-[var(--color-primary)] text-[var(--color-text-primary)] font-semibold'
                : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:border-[var(--color-border)]'
              }
            `}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Filter bar */}
      <FilterBar
        searchPlaceholder="태스크 검색..."
        searchValue={searchQuery}
        onSearchChange={(v) => { setSearchQuery(v); setPage(0); }}
      >
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="">Status: All</option>
          <option value="TODO">TODO</option>
          <option value="IN_PROGRESS">IN PROGRESS</option>
          <option value="DONE">DONE</option>
        </select>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="updatedAt">Sort: Updated</option>
          <option value="createdAt">Sort: Created</option>
          <option value="dueDate">Sort: Due Date</option>
        </select>
        <select
          value={direction}
          onChange={(e) => setDirection(e.target.value)}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          <option value="DESC">Direction: Desc</option>
          <option value="ASC">Direction: Asc</option>
        </select>
      </FilterBar>

      {/* Loading state */}
      {loading && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="p-[var(--spacing-base)] border-b border-[var(--color-border)] last:border-b-0 animate-pulse">
              <div className="h-4 bg-[var(--color-surface-muted)] rounded w-3/4 mb-2" />
              <div className="h-3 bg-[var(--color-surface-muted)] rounded w-1/2" />
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && tasks.length === 0 && (
        <div className="
          text-center py-[var(--spacing-xl)]
          text-[var(--color-text-muted)] text-[var(--text-sm)]
        ">
          {searchQuery || statusFilter
            ? '검색 결과가 없습니다.'
            : '아직 태스크가 없습니다. 첫 태스크를 생성하세요.'}
        </div>
      )}

      {/* Task list */}
      {!loading && tasks.length > 0 && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
          {tasks.map((task, index) => {
            const StatusIcon = statusIcons[task.status];
            return (
              <div
                key={task.taskId}
                onClick={() => setSelectedTaskId(task.taskId)}
                className={`
                  px-[var(--spacing-base)] py-[var(--spacing-md)] cursor-pointer
                  hover:bg-[var(--color-surface-muted)] transition-colors
                  ${index < tasks.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                `}
              >
                {/* Row 1: status icon + title + meta right */}
                <div className="flex items-center gap-[var(--spacing-sm)]">
                  <StatusIcon
                    size={16}
                    style={{ color: statusColors[task.status] }}
                    className="shrink-0"
                  />
                  <span className="text-[var(--text-xs)] text-[var(--color-text-muted)] font-mono shrink-0">
                    TASK-{task.taskId}
                  </span>
                  <span className="text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] truncate">
                    {task.title}
                  </span>
                  <div className="ml-auto flex items-center gap-[var(--spacing-md)] shrink-0">
                    {task.commentCount > 0 && (
                      <span className="flex items-center gap-[2px] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        <MessageSquare size={12} />
                        {task.commentCount}
                      </span>
                    )}
                  </div>
                </div>

                {/* Row 2: priority + assignee + due date */}
                <div className="flex items-center gap-[var(--spacing-md)] mt-[var(--spacing-xs)] ml-[24px]">
                  {/* Priority */}
                  <Badge variant={priorityVariant[task.priority] || 'muted'} size="sm">
                    {task.priority}
                  </Badge>

                  {/* Assignee */}
                  {task.assignee && (
                    <span className="flex items-center gap-[2px] text-[var(--text-xs)] text-[var(--color-text-secondary)]">
                      <div className="
                        w-[14px] h-[14px] rounded-full bg-[var(--color-primary)]
                        text-white text-[8px] font-semibold
                        flex items-center justify-center
                      ">
                        {task.assignee.nickname.charAt(0).toUpperCase()}
                      </div>
                      {task.assignee.nickname}
                    </span>
                  )}

                  {/* Due date */}
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

      {/* Task Detail Drawer */}
      {selectedTaskId && (
        <TaskDetailDrawer
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onStatusChange={handleStatusChange}
        />
      )}
    </div>
  );
}
