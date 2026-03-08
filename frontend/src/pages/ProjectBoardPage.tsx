import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, Plus, UserPlus } from 'lucide-react';
import BoardColumnComponent from '@/components/BoardColumn';
import FilterBar from '@/components/FilterBar';
import TaskCard from '@/components/TaskCard';
import TaskCreateModal from '@/components/TaskCreateModal';
import TaskDetailDrawer from '@/components/TaskDetailDrawer';
import apiClient from '@/api/client';
import { toProjectDetail } from '@/utils/projectMappers';
import type {
  ApiResponse,
  BoardColumn,
  BoardTaskCard,
  ProjectDetail,
  ProjectDetailResponse,
  TaskBoardResponse,
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
  const [priorityFilter, setPriorityFilter] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [showTaskModal, setShowTaskModal] = useState(false);

  const normalizeColumns = (rawColumns: TaskBoardResponse['columns']) =>
    columnOrder.map((status) => rawColumns.find((column) => column.status === status) ?? { status, tasks: [] });

  const fetchBoard = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [projectRes, boardRes] = await Promise.all([
        apiClient.get<ApiResponse<ProjectDetailResponse>>(`/projects/${projectId}`),
        apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`),
      ]);
      setProject(toProjectDetail(projectRes.data.data));
      setColumns(normalizeColumns(boardRes.data.data.columns));
    } catch (caughtError) {
      setError('Failed to load board.');
      console.error('Failed to fetch board:', caughtError);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    if (projectId) {
      void fetchBoard();
    }
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

  const refreshBoard = async () => {
    try {
      setError(null);
      const response = await apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`);
      setColumns(normalizeColumns(response.data.data.columns));
    } catch (caughtError) {
      setError('Failed to refresh board.');
      console.error('Failed to refresh board:', caughtError);
    }
  };

  const filterTasks = (tasks: BoardTaskCard[]) => {
    let result = tasks;
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (task) => task.title.toLowerCase().includes(query) || task.labels.some((label) => label.name.toLowerCase().includes(query))
      );
    }
    if (priorityFilter) {
      result = result.filter((task) => task.priority === priorityFilter);
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
        <div className="h-6 w-48 rounded bg-[var(--color-surface-muted)]" />
        <div className="h-4 w-96 rounded bg-[var(--color-surface-muted)]" />
        <div className="mt-6 flex gap-4">
          {[1, 2, 3].map((item) => (
            <div key={item} className="h-[300px] flex-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)]" />
          ))}
        </div>
      </div>
    );
  }

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
          <h1 className="m-0 text-[var(--text-lg)] font-bold text-[var(--color-text-primary)]">{project?.name}</h1>
          {project?.description && (
            <p className="m-0 mt-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
              {project.description}
            </p>
          )}
        </div>
        <div className="flex items-center gap-[var(--spacing-sm)]">
          <button
            type="button"
            onClick={() => navigate(`/projects/${projectId}/members?invite=1`)}
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
            type="button"
            onClick={() => setShowTaskModal(true)}
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

      <TaskCreateModal
        projectId={Number(projectId)}
        open={showTaskModal}
        onClose={() => setShowTaskModal(false)}
        onCreated={() => {
          void refreshBoard();
        }}
      />

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
        <div className="mt-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      <FilterBar searchPlaceholder="Search tasks..." searchValue={searchQuery} onSearchChange={setSearchQuery}>
        <select
          value={priorityFilter}
          onChange={(event) => setPriorityFilter(event.target.value)}
          className="
            h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
            rounded-[var(--radius-sm)] bg-[var(--color-surface)]
            text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:outline-none focus:border-[var(--color-primary)]
          "
        >
          {priorityOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </FilterBar>

      <div className="mt-[var(--spacing-sm)] flex gap-[var(--spacing-base)] overflow-x-auto pb-[var(--spacing-base)]">
        {columns.map((column) => {
          const filteredTasks = filterTasks(column.tasks);
          return (
            <BoardColumnComponent key={column.status} status={column.status} count={filteredTasks.length}>
              {filteredTasks.length === 0 ? (
                <div className="py-[var(--spacing-lg)] text-center text-[var(--text-xs)] text-[var(--color-text-muted)]">
                  No tasks
                </div>
              ) : (
                filteredTasks.map((task) => (
                  <TaskCard key={task.taskId} task={task} onClick={() => setSelectedTaskId(task.taskId)} />
                ))
              )}
            </BoardColumnComponent>
          );
        })}
      </div>

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
