import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, UserPlus } from 'lucide-react';
import BoardColumn from '@/components/BoardColumn';
import TaskCard from '@/components/TaskCard';
import FilterBar from '@/components/FilterBar';
import TaskDetailDrawer from '@/components/TaskDetailDrawer';
import apiClient from '@/api/client';
import type { ApiResponse, ProjectDetail, TaskBoardResponse, TaskSummary } from '@/types';

type TabType = 'board' | 'list' | 'members' | 'settings';

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [board, setBoard] = useState<TaskBoardResponse>({ todo: [], inProgress: [], done: [] });
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('board');

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true);
        const [projectRes, boardRes] = await Promise.all([
          apiClient.get<ApiResponse<ProjectDetail>>(`/projects/${projectId}`),
          apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`),
        ]);
        setProject(projectRes.data.data);
        setBoard(boardRes.data.data);
      } catch {
        // Error handling
      } finally {
        setLoading(false);
      }
    }
    if (projectId) fetchData();
  }, [projectId]);

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

  const handleMoveTask = async (taskId: number, newStatus: string) => {
    try {
      await apiClient.patch(`/tasks/${taskId}/move`, { status: newStatus });
      // Refresh board
      const res = await apiClient.get<ApiResponse<TaskBoardResponse>>(`/projects/${projectId}/tasks/board`);
      setBoard(res.data.data);
    } catch {
      // Error handling
    }
  };

  const filterTasks = (tasks: TaskSummary[]) => {
    if (!searchQuery) return tasks;
    const q = searchQuery.toLowerCase();
    return tasks.filter(
      (t) =>
        t.title.toLowerCase().includes(q) ||
        t.description?.toLowerCase().includes(q) ||
        t.labels.some((l) => l.name.toLowerCase().includes(q))
    );
  };

  const tabs: { key: TabType; label: string }[] = [
    { key: 'board', label: 'Board' },
    { key: 'list', label: 'List' },
    { key: 'members', label: 'Members' },
    { key: 'settings', label: 'Settings' },
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

      {/* Filter bar */}
      <FilterBar
        searchPlaceholder="태스크 검색..."
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
      >
        <select className="
          h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
          rounded-[var(--radius-sm)] bg-[var(--color-surface)]
          text-[var(--text-sm)] text-[var(--color-text-secondary)]
          focus:outline-none focus:border-[var(--color-primary)]
        ">
          <option value="">Assignee: All</option>
        </select>
        <select className="
          h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
          rounded-[var(--radius-sm)] bg-[var(--color-surface)]
          text-[var(--text-sm)] text-[var(--color-text-secondary)]
          focus:outline-none focus:border-[var(--color-primary)]
        ">
          <option value="">Label: All</option>
        </select>
        <select className="
          h-[32px] px-[var(--spacing-sm)] border border-[var(--color-border)]
          rounded-[var(--radius-sm)] bg-[var(--color-surface)]
          text-[var(--text-sm)] text-[var(--color-text-secondary)]
          focus:outline-none focus:border-[var(--color-primary)]
        ">
          <option value="">Priority: All</option>
        </select>
      </FilterBar>

      {/* Kanban Board */}
      <div className="flex gap-[var(--spacing-base)] mt-[var(--spacing-sm)] overflow-x-auto pb-[var(--spacing-base)]">
        <BoardColumn status="TODO" count={filterTasks(board.todo).length}>
          {filterTasks(board.todo).length === 0 ? (
            <div className="text-center py-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
              태스크 없음
            </div>
          ) : (
            filterTasks(board.todo).map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onClick={() => setSelectedTaskId(task.id)}
              />
            ))
          )}
        </BoardColumn>

        <BoardColumn status="IN_PROGRESS" count={filterTasks(board.inProgress).length}>
          {filterTasks(board.inProgress).length === 0 ? (
            <div className="text-center py-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
              태스크 없음
            </div>
          ) : (
            filterTasks(board.inProgress).map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onClick={() => setSelectedTaskId(task.id)}
              />
            ))
          )}
        </BoardColumn>

        <BoardColumn status="DONE" count={filterTasks(board.done).length}>
          {filterTasks(board.done).length === 0 ? (
            <div className="text-center py-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
              태스크 없음
            </div>
          ) : (
            filterTasks(board.done).map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onClick={() => setSelectedTaskId(task.id)}
              />
            ))
          )}
        </BoardColumn>
      </div>

      {/* Task Detail Drawer */}
      {selectedTaskId && (
        <TaskDetailDrawer
          taskId={selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onStatusChange={handleMoveTask}
        />
      )}
    </div>
  );
}
