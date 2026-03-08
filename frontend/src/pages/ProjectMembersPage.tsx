import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { AlertCircle, Shield, Trash2, UserPlus, X } from 'lucide-react';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import { toProjectDetail } from '@/utils/projectMappers';
import type {
  ApiResponse,
  InvitationSummary,
  ProjectDetail,
  ProjectDetailResponse,
  ProjectMember,
  ProjectRole,
} from '@/types';

type TabType = 'board' | 'list' | 'members' | 'settings';

const roleVariant: Record<ProjectRole, 'primary' | 'muted'> = {
  OWNER: 'primary',
  MEMBER: 'muted',
};

function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.substring(0, 10).split('-');
  return `${year}.${month}.${day}`;
}

export default function ProjectMembersPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState<TabType>('members');
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<ProjectRole>('MEMBER');
  const [inviteSubmitting, setInviteSubmitting] = useState(false);
  const [inviteError, setInviteError] = useState<string | null>(null);

  const closeInviteModal = useCallback(() => {
    setShowInviteModal(false);
    setInviteEmail('');
    setInviteRole('MEMBER');
    setInviteError(null);
    if (searchParams.get('invite') === '1') {
      const next = new URLSearchParams(searchParams);
      next.delete('invite');
      setSearchParams(next, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [projectRes, membersRes] = await Promise.all([
        apiClient.get<ApiResponse<ProjectDetailResponse>>(`/projects/${projectId}`),
        apiClient.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`),
      ]);
      setProject(toProjectDetail(projectRes.data.data));
      setMembers(membersRes.data.data);
    } catch (caughtError) {
      setError('Failed to load members.');
      console.error('Failed to fetch members:', caughtError);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    if (projectId) {
      void fetchData();
    }
  }, [projectId, fetchData]);

  useEffect(() => {
    if (searchParams.get('invite') === '1') {
      setShowInviteModal(true);
    }
  }, [searchParams]);

  const handleTabClick = (tab: TabType) => {
    if (tab === 'board') navigate(`/projects/${projectId}/board`);
    else if (tab === 'list') navigate(`/projects/${projectId}/tasks`);
    else if (tab === 'settings') navigate(`/projects/${projectId}/settings`);
    else setActiveTab(tab);
  };

  const handleRoleChange = async (memberId: number, newRole: ProjectRole) => {
    try {
      setError(null);
      await apiClient.patch(`/projects/${projectId}/members/${memberId}/role`, { role: newRole });
      setMembers((previousMembers) => previousMembers.map((member) => (
        member.memberId === memberId ? { ...member, role: newRole } : member
      )));
    } catch (caughtError) {
      setError('Failed to change role.');
      console.error('Failed to change role:', caughtError);
    }
  };

  const handleRemoveMember = async (memberId: number, nickname: string) => {
    if (!confirm(`${nickname} will be removed from this project. Continue?`)) return;
    try {
      setError(null);
      await apiClient.delete(`/projects/${projectId}/members/${memberId}`);
      setMembers((previousMembers) => previousMembers.filter((member) => member.memberId !== memberId));
    } catch (caughtError) {
      setError('Failed to remove member.');
      console.error('Failed to remove member:', caughtError);
    }
  };

  const handleInvite = async () => {
    if (!inviteEmail.trim()) return;
    setInviteSubmitting(true);
    setInviteError(null);
    try {
      await apiClient.post<ApiResponse<InvitationSummary>>(`/projects/${projectId}/invitations`, {
        email: inviteEmail.trim(),
        role: inviteRole,
      });
      closeInviteModal();
    } catch (caughtError: unknown) {
      const message = (caughtError as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setInviteError(message || 'Failed to invite member.');
      console.error('Failed to invite:', caughtError);
    } finally {
      setInviteSubmitting(false);
    }
  };

  const filteredMembers = searchQuery
    ? members.filter(
        (member) =>
          member.nickname.toLowerCase().includes(searchQuery.toLowerCase()) ||
          member.email.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : members;

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
          <h1 className="m-0 text-[var(--text-lg)] font-bold text-[var(--color-text-primary)]">{project?.name}</h1>
        </div>
        <button
          type="button"
          onClick={() => setShowInviteModal(true)}
          className="
            flex h-[32px] items-center gap-1 rounded-[var(--radius-sm)] border-none
            bg-[var(--color-primary)] px-[var(--spacing-md)] text-[var(--text-sm)] font-medium text-white
            hover:bg-[var(--color-primary-hover)]
          "
        >
          <UserPlus size={14} />
          Invite Member
        </button>
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
        <div className="mt-[var(--spacing-sm)] flex items-center gap-[var(--spacing-sm)] rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      <FilterBar searchPlaceholder="Search members..." searchValue={searchQuery} onSearchChange={setSearchQuery}>
        <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">{members.length} members</span>
      </FilterBar>

      {loading && (
        <div className="mt-[var(--spacing-sm)] space-y-3 animate-pulse">
          {[1, 2, 3].map((item) => (
            <div key={item} className="h-16 rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)]" />
          ))}
        </div>
      )}

      {!loading && (
        <div className="mt-[var(--spacing-sm)] overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
          {filteredMembers.length === 0 ? (
            <div className="py-[var(--spacing-xl)] text-center text-[var(--text-sm)] text-[var(--color-text-muted)]">
              {searchQuery ? 'No members matched your search.' : 'No members yet.'}
            </div>
          ) : (
            filteredMembers.map((member, index) => (
              <div
                key={member.memberId}
                className={`
                  flex items-center gap-[var(--spacing-base)] px-[var(--spacing-base)] py-[var(--spacing-md)]
                  ${index < filteredMembers.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                `}
              >
                <div className="flex h-[36px] w-[36px] shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-[var(--text-sm)] font-semibold text-white">
                  {member.nickname.charAt(0).toUpperCase()}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-[var(--spacing-sm)]">
                    <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">{member.nickname}</span>
                    <Badge variant={roleVariant[member.role]} size="sm">
                      <Shield size={10} className="mr-[2px] inline" />
                      {member.role}
                    </Badge>
                  </div>
                  <div className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                    {member.email} ¡¤ joined {formatDate(member.joinedAt)}
                  </div>
                </div>

                <div className="flex shrink-0 items-center gap-[var(--spacing-sm)]">
                  <select
                    value={member.role}
                    onChange={(event) => handleRoleChange(member.memberId, event.target.value as ProjectRole)}
                    className="
                      h-[28px] rounded-[var(--radius-sm)] border border-[var(--color-border)]
                      bg-[var(--color-surface)] px-[var(--spacing-xs)] text-[var(--text-xs)]
                      focus:outline-none focus:border-[var(--color-primary)]
                    "
                  >
                    <option value="OWNER">OWNER</option>
                    <option value="MEMBER">MEMBER</option>
                  </select>
                  <button
                    type="button"
                    onClick={() => handleRemoveMember(member.memberId, member.nickname)}
                    title="Remove Member"
                    className="rounded-[var(--radius-sm)] border-none bg-transparent p-[var(--spacing-xs)] text-[var(--color-danger)] hover:bg-[var(--color-accent-red)]"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {showInviteModal && (
        <>
          <div className="fixed inset-0 z-40 bg-black/30" onClick={closeInviteModal} />
          <div className="fixed inset-0 z-50 flex items-center justify-center p-[var(--spacing-base)]" onClick={closeInviteModal}>
            <div
              className="w-full max-w-[420px] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-[var(--spacing-lg)] shadow-lg"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="mb-[var(--spacing-base)] flex items-center justify-between">
                <h3 className="m-0 text-[var(--text-base)] font-bold text-[var(--color-text-primary)]">Invite Member</h3>
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
                  <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                    Email
                  </label>
                  <input
                    type="email"
                    value={inviteEmail}
                    onChange={(event) => setInviteEmail(event.target.value)}
                    placeholder="user@example.com"
                    className="h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--spacing-sm)] text-[var(--text-sm)]"
                  />
                </div>
                <div>
                  <label className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]">
                    Role
                  </label>
                  <select
                    value={inviteRole}
                    onChange={(event) => setInviteRole(event.target.value as ProjectRole)}
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
                    onClick={handleInvite}
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
    </div>
  );
}
