import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, UserPlus, Settings, Users, FolderKanban } from 'lucide-react';
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
        const [wsRes, membersRes] = await Promise.all([
          apiClient.get<ApiResponse<WorkspaceDetailResponse>>(`/workspaces/${workspaceId}`),
          apiClient.get<ApiResponse<WorkspaceMemberResponse[]>>(`/workspaces/${workspaceId}/members`),
        ]);
        setWorkspace(toWorkspaceDetail(wsRes.data.data));
        setMembers(membersRes.data.data.map(toWorkspaceMember));
      } catch {
        // Error handling
      } finally {
        setLoading(false);
      }
    }
    if (workspaceId) fetchData();
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
        <div className="h-6 bg-[var(--color-surface-muted)] rounded w-48 mb-2" />
        <div className="h-4 bg-[var(--color-surface-muted)] rounded w-96 mb-4" />
        <div className="h-8 bg-[var(--color-surface-muted)] rounded w-full" />
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="flex items-start justify-between pb-[var(--spacing-base)] border-b border-[var(--color-border)]">
        <div>
          <div className="flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)] mb-[var(--spacing-xs)]">
            <span className="cursor-pointer hover:text-[var(--color-primary)]" onClick={() => navigate('/workspaces')}>
              Workspaces
            </span>
            <span>/</span>
            <span className="text-[var(--color-text-primary)] font-medium">{workspace?.name}</span>
          </div>
          <h1 className="text-[var(--text-lg)] font-bold text-[var(--color-text-primary)] m-0">
            {workspace?.name}
          </h1>
          {workspace?.description && (
            <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] m-0">
              {workspace.description}
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
            New Project
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-[var(--color-border)] mt-[var(--spacing-base)]">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
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

      {/* Content */}
      <div className="mt-[var(--spacing-lg)] flex gap-[var(--spacing-lg)]">
        {/* Main Column */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-[var(--spacing-md)]">
            <h2 className="text-[var(--text-base)] font-semibold text-[var(--color-text-primary)] m-0">
              Projects
            </h2>
          </div>

          {/* Empty state for projects */}
          <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
            <div className="
              text-center py-[var(--spacing-xl)]
              text-[var(--color-text-muted)] text-[var(--text-sm)]
            ">
              아직 프로젝트가 없습니다. 첫 프로젝트를 생성하세요.
            </div>
          </div>
        </div>

        {/* Right Rail */}
        <div className="w-[280px] shrink-0">
          {/* Members */}
          <div className="mb-[var(--spacing-lg)]">
            <h3 className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)] mb-[var(--spacing-sm)] m-0 flex items-center gap-[var(--spacing-sm)]">
              <Users size={14} />
              Members
              <span className="text-[var(--color-text-muted)] font-normal">({members.length})</span>
            </h3>
            <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden">
              {members.length === 0 ? (
                <div className="p-[var(--spacing-base)] text-[var(--text-sm)] text-[var(--color-text-muted)] text-center">
                  멤버 없음
                </div>
              ) : (
                members.map((member, i) => (
                  <div
                    key={member.memberId}
                    className={`
                      flex items-center gap-[var(--spacing-sm)] px-[var(--spacing-md)] py-[var(--spacing-sm)]
                      ${i < members.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                    `}
                  >
                    <div className="
                      w-[24px] h-[24px] rounded-full bg-[var(--color-primary)]
                      text-white text-[var(--text-xs)] font-semibold
                      flex items-center justify-center shrink-0
                    ">
                      {member.nickname.charAt(0).toUpperCase()}
                    </div>
                    <span className="text-[var(--text-sm)] text-[var(--color-text-primary)] truncate flex-1">
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

          {/* Quick Info */}
          <div>
            <h3 className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)] mb-[var(--spacing-sm)] m-0">
              Info
            </h3>
            <div className="
              border border-[var(--color-border)] rounded-[var(--radius-md)]
              bg-[var(--color-surface)] p-[var(--spacing-base)]
              text-[var(--text-sm)] text-[var(--color-text-secondary)]
              space-y-[var(--spacing-sm)]
            ">
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
