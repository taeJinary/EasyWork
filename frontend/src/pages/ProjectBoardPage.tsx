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
  BoardTaskCard,
  ProjectDetail,
  ProjectDetailResponse,
  ProjectLabelResponse,
  TaskBoardResponse,
  TaskStatus,
} from '@/types';

type TabType = 'board' | 'list' | 'members' | 'settings';

const columnOrder: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [columns, setColumns] = useState<TaskBoardResponse['columns']>([]);
  const [labels, setLabels] = useState<ProjectLabelResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('board');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [labelFilter, setLabelFilter] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [showTaskModal, setShowTaskModal] = useState(false);

  const normalizeColumns = (rawColumns: TaskBoardResponse['columns']) =>
    columnOrder.map((status) => rawColumns.find((column) => column.status === status) ?? { status, tasks: [] });

  const fetchBoard = useCallback(async () => {
    if (!projectId) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const boardParams: Record<string, string> = {};
      if (labelFilter) boardParams.labelId = labelFilter;

      const [projectRes, boardRes, labelsRes] = await Promise.all([
        apiClient.get<ApiResponse<ProjectDetailResponse>>(`/projects/${projectId}`),
        apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`, {
          params: boardParams,
        }),
        apiClient.get<ApiResponse<ProjectLabelResponse[]>>(`/projects/${projectId}/labels`),
      ]);

      setProject(toProjectDetail(projectRes.data.data));
      setColumns(normalizeColumns(boardRes.data.data.columns));
      setLabels(labelsRes.data.data);
    } catch (caughtError) {
      setError('보드를 불러오지 못했습니다.');
      console.error('Failed to fetch board:', caughtError);
    } finally {
      setLoading(false);
    }
  }, [projectId, labelFilter]);

  useEffect(() => {
    if (projectId) {
      void fetchBoard();
    }
  }, [projectId, fetchBoard]);

  const handleTabClick = (tab: TabType) => {
    if (tab === 'list') {
      navigate(`/projects/${projectId}/tasks`);
      return;
    }
    if (tab === 'members') {
      navigate(`/projects/${projectId}/members`);
      return;
    }
    if (tab === 'settings') {
      navigate(`/projects/${projectId}/settings`);
      return;
    }
    setActiveTab(tab);
  };

  const refreshBoard = async () => {
    if (!projectId) {
      return;
    }

    try {
      setError(null);
      const boardParams: Record<string, string> = {};
      if (labelFilter) boardParams.labelId = labelFilter;

      const response = await apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`, {
        params: boardParams,
      });
      setColumns(normalizeColumns(response.data.data.columns));
    } catch (caughtError) {
      setError('보드를 새로고침하지 못했습니다.');
      console.error('Failed to refresh board:', caughtError);
    }
  };

  const filterTasks = (tasks: BoardTaskCard[]) => {
    let result = tasks;

    if (searchQuery) {
      const normalizedQuery = searchQuery.toLowerCase();
      result = result.filter(
        (task) =>
          task.title.toLowerCase().includes(normalizedQuery) ||
          task.labels.some((label) => label.name.toLowerCase().includes(normalizedQuery))
      );
    }

    if (priorityFilter) {
      result = result.filter((task) => task.priority === priorityFilter);
    }

    return result;
  };

  const tabs: { key: TabType; label: string }[] = [
    { key: 'board', label: '보드' },
    { key: 'list', label: '목록' },
    { key: 'members', label: '멤버' },
    { key: 'settings', label: '설정' },
  ];

  const priorityOptions = [
    { value: '', label: '우선순위: 전체' },
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
              작업공간
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
              flex h-[32px] cursor-pointer items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
              bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-primary)]
              hover:bg-[var(--color-surface-muted)]
            "
          >
            <UserPlus size={14} />
            멤버 초대
          </button>
          <button
            type="button"
            onClick={() => setShowTaskModal(true)}
            className="
              flex h-[32px] cursor-pointer items-center gap-1 rounded-[var(--radius-sm)] border-none
              bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
              hover:bg-[var(--color-primary-hover)]
            "
          >
            <Plus size={14} />
            새 작업
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

      <FilterBar searchPlaceholder="작업 검색..." searchValue={searchQuery} onSearchChange={setSearchQuery}>
        <select
          value={priorityFilter}
          onChange={(event) => setPriorityFilter(event.target.value)}
          className="
            h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:border-[var(--color-primary)] focus:outline-none
          "
        >
          {priorityOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          aria-label="라벨 필터"
          value={labelFilter}
          onChange={(event) => setLabelFilter(event.target.value)}
          className="
            h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
            bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
            focus:border-[var(--color-primary)] focus:outline-none
          "
        >
          <option value="">라벨: 전체</option>
          {labels.map((label) => (
            <option key={label.labelId} value={String(label.labelId)}>
              {label.name}
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
                  작업이 없습니다
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
          onTaskUpdated={refreshBoard}
          onTaskDeleted={() => {
            setSelectedTaskId(null);
            void refreshBoard();
          }}
        />
      )}
    </div>
  );
}
