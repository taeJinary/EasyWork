import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, UserPlus, AlertCircle } from 'lucide-react';
import BoardColumnComponent from '@/components/BoardColumn';
import TaskCard from '@/components/TaskCard';
import FilterBar from '@/components/FilterBar';
import TaskDetailDrawer from '@/components/TaskDetailDrawer';
import apiClient from '@/api/client';
import { toProjectDetail } from '@/utils/projectMappers';
import type {
  ApiResponse,
  ProjectDetail,
  ProjectDetailResponse,
  TaskBoardResponse,
  BoardColumn,
  BoardTaskCard,
  TaskStatus,
} from '@/types';

type TabType = 'board' | 'list' | 'members' | 'settings';

const columnOrder: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [columns, setColumns] = useState<BoardColumn[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('board');
  // [Bug 4 fix] filter states
  const [priorityFilter, setPriorityFilter] = useState<string>('');
  // [Warn 4 fix] error state
  const [error, setError] = useState<string | null>(null);

  const fetchBoard = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [projectRes, boardRes] = await Promise.all([
        apiClient.get<ApiResponse<ProjectDetailResponse>>(`/projects/${projectId}`),
        apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`),
      ]);
      setProject(toProjectDetail(projectRes.data.data));
      const rawColumns = boardRes.data.data.columns;
      const normalized = columnOrder.map((status) => {
        const found = rawColumns.find((c) => c.status === status);
        return found ?? { status, tasks: [] };
      });
      setColumns(normalized);
    } catch (err) {
      setError('보드를 불러오는 데 실패했습니다.');
      console.error('Failed to fetch board:', err);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    if (projectId) fetchBoard();
  }, [projectId, fetchBoard]);

  const handleTabClick = (tab: TabType) => {
    if (tab === 'list') {
      navigate(`/projects/${projectId}/tasks`);
    } else if (tab === 'members') {
      navigate(`/projects/${projectId}/members`);
    } else if (tab === 'settings') {
      navigate(`/projects/${projectId}/settings`);
    } else {
      setActiveTab(tab);
    }
  };

  // Called by TaskDetailDrawer AFTER it already PATCHes /tasks/{id}/move.
  // This callback only refreshes the board columns — no duplicate PATCH.
  const refreshBoard = async () => {
    try {
      setError(null);
      const res = await apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`);
      const rawColumns = res.data.data.columns;
      const normalized = columnOrder.map((status) => {
        const found = rawColumns.find((c) => c.status === status);
        return found ?? { status, tasks: [] };
      });
      setColumns(normalized);
    } catch (err) {
      setError('보드 새로고침에 실패했습니다.');
      console.error('Failed to refresh board:', err);
    }
  };

  // [Bug 4 fix] filterTasks now checks searchQuery + priorityFilter
  const filterTasks = (tasks: BoardTaskCard[]) => {
    let result = tasks;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (t) =>
          t.title.toLowerCase().includes(q) ||
          t.labels.some((l) => l.name.toLowerCase().includes(q))
      );
    }
    if (priorityFilter) {
      result = result.filter((t) => t.priority === priorityFilter);
    }
    return result;
  };

  const tabs: { key: TabType; label: string }[] = [
    { key: 'board', label: 'Board' },
    { key: 'list', label: 'List' },
    { key: 'members', label: 'Members' },
    { key: 'settings', label: 'Settings' },
  ];

  const priorityOptions: { value: string; label: string }[] = [
    { value: '', label: 'Priority: All' },
    { value: 'URGENT', label: 'URGENT' },
    { value: 'HIGH', label: 'HIGH' },
    { value: 'MEDIUM', label: 'MEDIUM' },
    { value: 'LOW', label: 'LOW' },
  ];

  if (loading) {
    return (
      <div className="animate-pulse space-y-4">
        <div className="h-6 bg-[var(--color-surface-muted)] rounded w-48" />
        <div className="h-4 bg-[var(--color-surface-muted)] rounded w-96" />
        <div className="flex gap-4 mt-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex-1 h-[300px] bg-[var(--color-surface-muted)] rounded-[var(--radius-md)]" />
          ))}
        </div>
      </div>
    );
  }

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

      {/* [Warn 4 fix] Error banner */}
      {error && (
        <div className="
          flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)]
          bg-[var(--color-accent-red)] border border-[var(--color-danger)]
          rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]
        ">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      {/* Filter bar — [Bug 4 fix] filters wired to state */}
      <FilterBar
        searchPlaceholder="태스크 검색..."
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
      >
        <select
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          {priorityOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </FilterBar>

      {/* Kanban Board — iterate columns */}
      <div className="flex gap-[var(--spacing-base)] mt-[var(--spacing-sm)] overflow-x-auto pb-[var(--spacing-base)]">
        {columns.map((col) => {
          const filtered = filterTasks(col.tasks);
          return (
            <BoardColumnComponent key={col.status} status={col.status} count={filtered.length}>
              {filtered.length === 0 ? (
                <div className="text-center py-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                  태스크 없음
                </div>
              ) : (
                filtered.map((task) => (
                  <TaskCard
                    key={task.taskId}
                    task={task}
                    onClick={() => setSelectedTaskId(task.taskId)}
                  />
                ))
              )}
            </BoardColumnComponent>
          );
        })}
      </div>

      {/* Task Detail Drawer */}
      {selectedTaskId && (
        <TaskDetailDrawer
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onStatusChange={refreshBoard}
        />
      )}
    </div>
  );
}
