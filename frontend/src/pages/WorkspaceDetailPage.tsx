import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, FolderKanban, Mail, Plus, Settings, UserPlus, Users, X } from 'lucide-react';
import Badge from '@/components/Badge';
import ProjectCreateModal from '@/components/ProjectCreateModal';
import apiClient from '@/api/client';
import { toProjectSummary } from '@/utils/projectMappers';
import type {
  ApiResponse,
  ProjectListItemResponse,
  ProjectSummary,
  WorkspaceDetail,
  WorkspaceDetailResponse,
  WorkspaceInvitationAction,
  WorkspaceSentInvitationListItem,
  WorkspaceInvitationSummary,
  WorkspaceMember,
  WorkspaceMemberResponse,
  WorkspaceRole,
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

function formatDate(dateStr: string): string {
  const [y, m, d] = dateStr.substring(0, 10).split('-');
  return `${y}.${m}.${d}`;
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
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showProjectModal, setShowProjectModal] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<WorkspaceRole>('MEMBER');
  const [inviteSubmitting, setInviteSubmitting] = useState(false);
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [inviteListError, setInviteListError] = useState<string | null>(null);
  const [sentInvitations, setSentInvitations] = useState<WorkspaceSentInvitationListItem[]>([]);
  const [cancelingInvitationId, setCancelingInvitationId] = useState<number | null>(null);

  const loadWorkspaceData = useCallback(async () => {
    if (!workspaceId) {
      return;
    }

    try {
      setLoading(true);
      setError('');
      setInviteListError(null);
      const [workspaceResponse, membersResponse, projectsResponse, invitationsResponse] = await Promise.all([
        apiClient.get<ApiResponse<WorkspaceDetailResponse>>(`/workspaces/${workspaceId}`),
        apiClient.get<ApiResponse<WorkspaceMemberResponse[]>>(`/workspaces/${workspaceId}/members`),
        apiClient.get<ApiResponse<ProjectListItemResponse[]>>(`/workspaces/${workspaceId}/projects`),
        apiClient.get<ApiResponse<WorkspaceSentInvitationListItem[]>>(`/workspaces/${workspaceId}/invitations`, {
          params: { status: 'PENDING' },
        }),
      ]);
      setWorkspace(toWorkspaceDetail(workspaceResponse.data.data));
      setMembers(membersResponse.data.data.map(toWorkspaceMember));
      setProjects(projectsResponse.data.data.map(toProjectSummary));
      setSentInvitations(invitationsResponse.data.data);
    } catch {
      setWorkspace(null);
      setMembers([]);
      setProjects([]);
      setSentInvitations([]);
      setError('Failed to load workspace.');
    } finally {
      setLoading(false);
    }
  }, [workspaceId]);

  useEffect(() => {
    void loadWorkspaceData();
  }, [loadWorkspaceData]);

  const closeInviteModal = useCallback(() => {
    setShowInviteModal(false);
    setInviteEmail('');
    setInviteRole('MEMBER');
    setInviteError(null);
  }, []);

  const handleInvite = useCallback(async () => {
    if (!workspaceId || !inviteEmail.trim()) {
      return;
    }

    setInviteSubmitting(true);
    setInviteError(null);

    try {
      const response = await apiClient.post<ApiResponse<WorkspaceInvitationSummary>>(`/workspaces/${workspaceId}/invitations`, {
        email: inviteEmail.trim(),
        role: inviteRole,
      });
      setSentInvitations((current) => [
        {
          invitationId: response.data.data.invitationId,
          workspaceId: response.data.data.workspaceId,
          inviteeUserId: response.data.data.inviteeUserId,
          inviteeEmail: response.data.data.inviteeEmail,
          inviteeNickname: response.data.data.inviteeNickname,
          role: response.data.data.role,
          status: response.data.data.status,
          expiresAt: response.data.data.expiresAt,
          createdAt: new Date().toISOString(),
        },
        ...current,
      ]);
      closeInviteModal();
      setActiveTab('members');
    } catch (caughtError: unknown) {
      const message = (caughtError as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setInviteError(message || 'Failed to invite workspace member.');
      console.error('Failed to invite workspace member:', caughtError);
    } finally {
      setInviteSubmitting(false);
    }
  }, [closeInviteModal, inviteEmail, inviteRole, workspaceId]);

  const handleCancelInvitation = useCallback(async (invitation: WorkspaceSentInvitationListItem) => {
    if (!workspaceId) {
      return;
    }

    setCancelingInvitationId(invitation.invitationId);
    setInviteListError(null);

    try {
      const response = await apiClient.post<ApiResponse<WorkspaceInvitationAction>>(
        `/workspaces/${workspaceId}/invitations/${invitation.invitationId}/cancel`
      );
      setSentInvitations((current) =>
        current.filter((item) =>
          item.invitationId !== response.data.data.invitationId
        )
      );
    } catch (caughtError: unknown) {
      const message = (caughtError as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setInviteListError(message || 'Failed to cancel workspace invitation.');
      console.error('Failed to cancel workspace invitation:', caughtError);
    } finally {
      setCancelingInvitationId(null);
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

  if (error) {
    return (
      <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-lg)]">
        <p className="m-0 text-[var(--text-sm)] text-[var(--color-danger)]">{error}</p>
        <button
          type="button"
          onClick={() => void loadWorkspaceData()}
          className="
            mt-[var(--spacing-md)] flex h-[32px] items-center rounded-[var(--radius-sm)] border border-[var(--color-danger)]
            bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-danger)]
            hover:bg-[var(--color-surface-muted)]
          "
        >
          Retry
        </button>
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
            type="button"
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border border-[var(--color-border)]
              bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-primary)]
              hover:bg-[var(--color-surface-muted)]
            "
            onClick={() => {
              setActiveTab('members');
              setShowInviteModal(true);
            }}
          >
            <UserPlus size={14} />
            Invite
          </button>
          <button
            type="button"
            className="
              flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border-none
              bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
              hover:bg-[var(--color-primary-hover)]
            "
            onClick={() => setShowProjectModal(true)}
          >
            <Plus size={14} />
            New Project
          </button>
        </div>
      </div>

      <ProjectCreateModal
        fixedWorkspaceId={workspace?.id}
        open={showProjectModal}
        onClose={() => setShowProjectModal(false)}
        onCreated={(project) => navigate(`/projects/${project.projectId}/board`)}
      />

      {showInviteModal && (
        <>
          <div className="fixed inset-0 z-40 bg-black/30" onClick={closeInviteModal} />
          <div
            className="fixed inset-0 z-50 flex items-center justify-center p-[var(--spacing-base)]"
            onClick={closeInviteModal}
          >
            <div
              className="w-full max-w-[420px] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-lg)] shadow-lg"
              role="dialog"
              aria-modal="true"
              aria-labelledby="workspace-invite-dialog-title"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="mb-[var(--spacing-base)] flex items-center justify-between">
                <h3
                  id="workspace-invite-dialog-title"
                  className="m-0 text-[var(--text-base)] font-bold text-[var(--color-text-primary)]"
                >
                  Invite Workspace Member
                </h3>
                <button
                  type="button"
                  onClick={closeInviteModal}
                  className="border-none bg-transparent p-[var(--spacing-xs)] text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)]"
                >
                  <X size={16} />
                </button>
              </div>

              {inviteError && (
                <div className="mb-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                  <AlertCircle size={14} />
                  {inviteError}
                </div>
              )}

              <div className="space-y-[var(--spacing-md)]">
                <div>
                  <label
                    htmlFor="workspace-invite-email"
                    className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
                  >
                    Email
                  </label>
                  <input
                    id="workspace-invite-email"
                    type="email"
                    value={inviteEmail}
                    onChange={(event) => setInviteEmail(event.target.value)}
                    placeholder="user@example.com"
                    className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
                  />
                </div>
                <div>
                  <label
                    htmlFor="workspace-invite-role"
                    className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
                  >
                    Role
                  </label>
                  <select
                    id="workspace-invite-role"
                    value={inviteRole}
                    onChange={(event) => setInviteRole(event.target.value as WorkspaceRole)}
                    className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
                  >
                    <option value="MEMBER">MEMBER</option>
                    <option value="OWNER">OWNER</option>
                  </select>
                </div>
                <div className="flex justify-end gap-[var(--spacing-sm)] pt-[var(--spacing-sm)]">
                  <button
                    type="button"
                    onClick={closeInviteModal}
                    className="h-[32px] rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-secondary)]"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={() => void handleInvite()}
                    disabled={!inviteEmail.trim() || inviteSubmitting}
                    className="h-[32px] rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white disabled:opacity-50"
                  >
                    {inviteSubmitting ? 'Sending...' : 'Invite'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </>
      )}

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
            {projects.length === 0 ? (
              <div className="py-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
                No projects yet. Create the first project in this workspace.
              </div>
            ) : (
              projects.map((project, index) => (
                <div
                  key={project.id}
                  onClick={() => navigate(`/projects/${project.id}/board`)}
                  className={`
                    flex cursor-pointer items-center px-[var(--spacing-base)] py-[var(--spacing-md)] transition-colors hover:bg-[var(--color-surface-muted)]
                    ${index < projects.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                  `}
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-[var(--spacing-sm)]">
                      <span className="text-[var(--text-sm)] font-semibold text-[var(--color-primary)]">{project.name}</span>
                      <Badge variant={project.myRole === 'OWNER' ? 'warning' : 'default'} size="sm">
                        {project.myRole}
                      </Badge>
                    </div>
                    {project.description && (
                      <div className="mt-[2px] truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        {project.description}
                      </div>
                    )}
                  </div>

                  <div className="flex shrink-0 items-center gap-[var(--spacing-lg)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                    <span>{project.memberCount} members</span>
                    <span>{project.openTaskCount} open tasks</span>
                    <span>Updated {formatTimeAgo(project.updatedAt)}</span>
                  </div>
                </div>
              ))
            )}
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

          {workspace?.myRole === 'OWNER' && (
            <div className="mb-[var(--spacing-lg)]">
              <h3 className="m-0 mb-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                <Mail size={14} />
                Pending Invites
                <span className="font-normal text-[var(--color-text-muted)]">({sentInvitations.length})</span>
              </h3>
              {inviteListError && (
                <div className="mb-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                  <AlertCircle size={14} />
                  {inviteListError}
                </div>
              )}
              <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
                {sentInvitations.length === 0 ? (
                  <div className="p-[var(--spacing-base)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
                    No pending invitations
                  </div>
                ) : (
                  sentInvitations.map((invitation, index) => (
                    <div
                      key={invitation.invitationId}
                      className={`px-[var(--spacing-md)] py-[var(--spacing-sm)] ${index < sentInvitations.length - 1 ? 'border-b border-[var(--color-border)]' : ''}`}
                    >
                      <div className="flex items-center justify-between gap-[var(--spacing-sm)]">
                        <div className="min-w-0">
                          <div className="truncate text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                            {invitation.inviteeNickname}
                          </div>
                          <div className="truncate text-[var(--text-xs)] text-[var(--color-text-muted)]">
                            {invitation.inviteeEmail}
                          </div>
                        </div>
                        <Badge variant={invitation.role === 'OWNER' ? 'warning' : 'default'} size="sm">
                          {invitation.role}
                        </Badge>
                      </div>
                      <div className="mt-[var(--spacing-xs)] flex items-center justify-between gap-[var(--spacing-sm)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                        <span>Expires {formatDate(invitation.expiresAt)}</span>
                        <button
                          type="button"
                          onClick={() => void handleCancelInvitation(invitation)}
                          disabled={cancelingInvitationId === invitation.invitationId}
                          className="h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-transparent px-[var(--spacing-sm)] text-[var(--text-xs)] font-medium text-[var(--color-danger)] hover:bg-[var(--color-accent-red)] disabled:opacity-50"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

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
