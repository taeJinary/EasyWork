import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FolderKanban, Plus, Settings, UserPlus, Users } from 'lucide-react';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import type {
  ApiResponse,
  WorkspaceDetail,
  WorkspaceDetailResponse,
  WorkspaceMember,
  WorkspaceMemberResponse,
} from '@/types';

type TabType = 'overview' | 'projects' | 'members' | 'settings';

function formatTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function toWorkspaceDetail(workspace: WorkspaceDetailResponse): WorkspaceDetail {
  return {
    id: workspace.workspaceId,
    name: workspace.name,
    description: workspace.description,
    memberCount: workspace.memberCount,
    myRole: workspace.myRole,
    updatedAt: workspace.updatedAt,
  };
}

function toWorkspaceMember(member: WorkspaceMemberResponse): WorkspaceMember {
  return {
    memberId: member.memberId,
    userId: member.userId,
    email: member.email,
    nickname: member.nickname,
    role: member.role,
    joinedAt: member.joinedAt,
  };
}

export default function WorkspaceDetailPage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const [workspace, setWorkspace] = useState<WorkspaceDetail | null>(null);
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true);
        const [workspaceResponse, membersResponse] = await Promise.all([
          apiClient.get<ApiResponse<WorkspaceDetailResponse>>(`/workspaces/${workspaceId}`),
          apiClient.get<ApiResponse<WorkspaceMemberResponse[]>>(`/workspaces/${workspaceId}/members`),
        ]);
        setWorkspace(toWorkspaceDetail(workspaceResponse.data.data));
        setMembers(membersResponse.data.data.map(toWorkspaceMember));
      } catch {
        // TODO: add dedicated error view.
      } finally {
        setLoading(false);
      }
    }

    if (workspaceId) {
      void fetchData();
    }
  }, [workspaceId]);

  const tabs: { key: TabType; label: string }[] = [
    { key: 'overview', label: 'Overview' },
    { key: 'projects', label: 'Projects' },
    { key: 'members', label: 'Members' },
    { key: 'settings', label: 'Settings' },
  ];

  if (loading) {
    return (
      <div className="animate-pulse">
        <div className="mb-2 h-6 w-48 rounded bg-[var(--color-surface-muted)]" />
        <div className="mb-4 h-4 w-96 rounded bg-[var(--color-surface-muted)]" />
        <div className="h-8 w-full rounded bg-[var(--color-surface-muted)]" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-start justify-between border-b border-[var(--color-border)] pb-[var(--spacing-base)]">
        <div>
          <div className="mb-[var(--spacing-xs)] flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)]">
            <span
              className="cursor-pointer hover:text-[var(--color-primary)]"
              onClick={() => navigate('/workspaces')}
            >
              Workspaces
            </span>
            <span>/</span>
            <span className="font-medium text-[var(--color-text-primary)]">{workspace?.name}</span>
          </div>
          <h1 className="m-0 text-[var(--text-lg)] font-bold text-[var(--color-text-primary)]">
            {workspace?.name}
          </h1>
          {workspace?.description && (
            <p className="m-0 mt-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
              {workspace.description}
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
            New Project
          </button>
        </div>
      </div>

      <div className="mt-[var(--spacing-base)] flex border-b border-[var(--color-border)]">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
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

      <div className="mt-[var(--spacing-lg)] flex gap-[var(--spacing-lg)]">
        <div className="min-w-0 flex-1">
          <div className="mb-[var(--spacing-md)] flex items-center justify-between">
            <h2 className="m-0 text-[var(--text-base)] font-semibold text-[var(--color-text-primary)]">
              Projects
            </h2>
          </div>

          <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
            <div className="py-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
              No projects yet. Create the first project in this workspace.
            </div>
          </div>
        </div>

        <div className="w-[280px] shrink-0">
          <div className="mb-[var(--spacing-lg)]">
            <h3 className="m-0 mb-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
              <Users size={14} />
              Members
              <span className="font-normal text-[var(--color-text-muted)]">({members.length})</span>
            </h3>
            <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
              {members.length === 0 ? (
                <div className="p-[var(--spacing-base)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
                  No members
                </div>
              ) : (
                members.map((member, index) => (
                  <div
                    key={member.memberId}
                    className={`
                      flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-md)] py-[var(--spacing-sm)]
                      ${index < members.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                    `}
                  >
                    <div
                      className="
                        flex h-[24px] w-[24px] shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)]
                        text-[var(--text-xs)] font-semibold text-white
                      "
                    >
                      {member.nickname.charAt(0).toUpperCase()}
                    </div>
                    <span className="flex-1 truncate text-[var(--text-sm)] text-[var(--color-text-primary)]">
                      {member.nickname}
                    </span>
                    <Badge variant={member.role === 'OWNER' ? 'warning' : 'default'}>
                      {member.role}
                    </Badge>
                  </div>
                ))
              )}
            </div>
          </div>

          <div>
            <h3 className="m-0 mb-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
              Info
            </h3>
            <div
              className="
                space-y-[var(--spacing-sm)] rounded-[var(--radius-md)] border border-[var(--color-border)]
                bg-[var(--color-surface)] p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
              "
            >
              <div className="flex items-center gap-[var(--spacing-sm)]">
                <Users size={14} className="text-[var(--color-text-muted)]" />
                <span>{workspace?.memberCount ?? 0} members</span>
              </div>
              <div className="flex items-center gap-[var(--spacing-sm)]">
                <FolderKanban size={14} className="text-[var(--color-text-muted)]" />
                <span>Role {workspace?.myRole ?? 'MEMBER'}</span>
              </div>
              {workspace?.updatedAt && (
                <div className="flex items-center gap-[var(--spacing-sm)]">
                  <Settings size={14} className="text-[var(--color-text-muted)]" />
                  <span>Updated {formatTimeAgo(workspace.updatedAt)}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
