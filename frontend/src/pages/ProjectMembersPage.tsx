import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { UserPlus, Shield, Trash2, AlertCircle, X } from 'lucide-react';
import FilterBar from '@/components/FilterBar';
import Badge from '@/components/Badge';
import apiClient from '@/api/client';
import type { ApiResponse, ProjectDetail, ProjectMember, ProjectRole, InvitationSummary } from '@/types';

type TabType = 'board' | 'list' | 'members' | 'settings';

const roleVariant: Record<ProjectRole, 'primary' | 'muted'> = {
  OWNER: 'primary',
  MEMBER: 'muted',
};

function formatDate(dateStr: string): string {
  const [y, m, d] = dateStr.substring(0, 10).split('-');
  return `${y}.${m}.${d}`;
}

export default function ProjectMembersPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState<TabType>('members');

  // Invite modal
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<ProjectRole>('MEMBER');
  const [inviteSubmitting, setInviteSubmitting] = useState(false);
  const [inviteError, setInviteError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [projectRes, membersRes] = await Promise.all([
        apiClient.get<ApiResponse<ProjectDetail>>(`/projects/${projectId}`),
        apiClient.get<ApiResponse<ProjectMember[]>>(`/projects/${projectId}/members`),
      ]);
      setProject(projectRes.data.data);
      setMembers(membersRes.data.data);
    } catch (err) {
      setError('멤버 목록을 불러오는 데 실패했습니다.');
      console.error('Failed to fetch members:', err);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    if (projectId) fetchData();
  }, [projectId, fetchData]);

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
      setMembers((prev) => prev.map((m) => m.memberId === memberId ? { ...m, role: newRole } : m));
    } catch (err) {
      setError('역할 변경에 실패했습니다.');
      console.error('Failed to change role:', err);
    }
  };

  const handleRemoveMember = async (memberId: number, nickname: string) => {
    if (!confirm(`${nickname}님을 프로젝트에서 제거하시겠습니까?`)) return;
    try {
      setError(null);
      await apiClient.delete(`/projects/${projectId}/members/${memberId}`);
      setMembers((prev) => prev.filter((m) => m.memberId !== memberId));
    } catch (err) {
      setError('멤버 제거에 실패했습니다.');
      console.error('Failed to remove member:', err);
    }
  };

  const handleInvite = async () => {
    if (!inviteEmail.trim()) return;
    setInviteSubmitting(true);
    setInviteError(null);
    try {
      await apiClient.post<ApiResponse<InvitationSummary>>(`/projects/${projectId}/invitations`, {
        email: inviteEmail,
        role: inviteRole,
      });
      setShowInviteModal(false);
      setInviteEmail('');
      setInviteRole('MEMBER');
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setInviteError(message || '초대에 실패했습니다.');
      console.error('Failed to invite:', err);
    } finally {
      setInviteSubmitting(false);
    }
  };

  const filteredMembers = searchQuery
    ? members.filter(
        (m) =>
          m.nickname.toLowerCase().includes(searchQuery.toLowerCase()) ||
          m.email.toLowerCase().includes(searchQuery.toLowerCase())
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
      {/* Header */}
      <div className="flex items-start justify-between pb-[var(--spacing-base)] border-b border-[var(--color-border)]">
        <div>
          <div className="flex items-center gap-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-muted)] mb-[var(--spacing-xs)]">
            <span className="cursor-pointer hover:text-[var(--color-primary)]" onClick={() => navigate('/workspaces')}>
              Workspace
            </span>
            <span>/</span>
            <span className="text-[var(--color-text-primary)] font-medium">{project?.name}</span>
          </div>
          <h1 className="text-[var(--text-lg)] font-bold text-[var(--color-text-primary)] m-0">{project?.name}</h1>
        </div>
        <button
          onClick={() => setShowInviteModal(true)}
          className="
            flex items-center gap-1 h-[32px] px-[var(--spacing-md)]
            bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
            text-[var(--text-sm)] font-medium border-none cursor-pointer
            hover:bg-[var(--color-primary-hover)]
          "
        >
          <UserPlus size={14} />
          Invite Member
        </button>
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

      {/* Error */}
      {error && (
        <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      {/* Filter */}
      <FilterBar searchPlaceholder="멤버 검색..." searchValue={searchQuery} onSearchChange={setSearchQuery}>
        <span className="text-[var(--text-sm)] text-[var(--color-text-muted)]">
          {members.length}명의 멤버
        </span>
      </FilterBar>

      {/* Loading */}
      {loading && (
        <div className="animate-pulse space-y-3 mt-[var(--spacing-sm)]">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 bg-[var(--color-surface-muted)] rounded-[var(--radius-sm)]" />
          ))}
        </div>
      )}

      {/* Member list */}
      {!loading && (
        <div className="border border-[var(--color-border)] rounded-[var(--radius-md)] bg-[var(--color-surface)] overflow-hidden mt-[var(--spacing-sm)]">
          {filteredMembers.length === 0 ? (
            <div className="text-center py-[var(--spacing-xl)] text-[var(--color-text-muted)] text-[var(--text-sm)]">
              {searchQuery ? '검색 결과가 없습니다.' : '멤버가 없습니다.'}
            </div>
          ) : (
            filteredMembers.map((member, idx) => (
              <div
                key={member.memberId}
                className={`
                  flex items-center gap-[var(--spacing-base)] px-[var(--spacing-base)] py-[var(--spacing-md)]
                  ${idx < filteredMembers.length - 1 ? 'border-b border-[var(--color-border)]' : ''}
                `}
              >
                {/* Avatar */}
                <div className="
                  w-[36px] h-[36px] rounded-full bg-[var(--color-primary)]
                  text-white text-[var(--text-sm)] font-semibold
                  flex items-center justify-center shrink-0
                ">
                  {member.nickname.charAt(0).toUpperCase()}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-[var(--spacing-sm)]">
                    <span className="text-[var(--text-sm)] font-semibold text-[var(--color-text-primary)]">
                      {member.nickname}
                    </span>
                    <Badge variant={roleVariant[member.role]} size="sm">
                      <Shield size={10} className="inline mr-[2px]" />
                      {member.role}
                    </Badge>
                  </div>
                  <div className="text-[var(--text-xs)] text-[var(--color-text-muted)]">
                    {member.email} · joined {formatDate(member.joinedAt)}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-[var(--spacing-sm)] shrink-0">
                  <select
                    value={member.role}
                    onChange={(e) => handleRoleChange(member.memberId, e.target.value as ProjectRole)}
                    className="
                      h-[28px] px-[var(--spacing-xs)]
                      border border-[var(--color-border)] rounded-[var(--radius-sm)]
                      bg-[var(--color-surface)] text-[var(--text-xs)]
                      focus:outline-none focus:border-[var(--color-primary)]
                    "
                  >
                    <option value="OWNER">OWNER</option>
                    <option value="MEMBER">MEMBER</option>
                  </select>
                  <button
                    onClick={() => handleRemoveMember(member.memberId, member.nickname)}
                    title="멤버 제거"
                    className="
                      p-[var(--spacing-xs)] rounded-[var(--radius-sm)]
                      text-[var(--color-danger)] hover:bg-[var(--color-accent-red)]
                      bg-transparent border-none cursor-pointer
                    "
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Invite Modal */}
      {showInviteModal && (
        <>
          <div className="fixed inset-0 bg-black/30 z-40" onClick={() => setShowInviteModal(false)} />
          <div className="
            fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2
            w-[420px] max-w-[90vw] bg-[var(--color-surface)]
            border border-[var(--color-border)] rounded-[var(--radius-md)]
            shadow-lg z-50 p-[var(--spacing-lg)]
          ">
            <div className="flex items-center justify-between mb-[var(--spacing-base)]">
              <h3 className="text-[var(--text-base)] font-bold text-[var(--color-text-primary)] m-0">
                멤버 초대
              </h3>
              <button
                onClick={() => setShowInviteModal(false)}
                className="p-[var(--spacing-xs)] bg-transparent border-none cursor-pointer text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)]"
              >
                <X size={16} />
              </button>
            </div>

            {inviteError && (
              <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mb-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
                <AlertCircle size={14} />
                {inviteError}
              </div>
            )}

            <div className="space-y-[var(--spacing-md)]">
              <div>
                <label className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                  이메일
                </label>
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="user@example.com"
                  className="
                    w-full h-[36px] px-[var(--spacing-sm)]
                    border border-[var(--color-border)] rounded-[var(--radius-sm)]
                    bg-[var(--color-surface)] text-[var(--text-sm)]
                    focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
                  "
                />
              </div>
              <div>
                <label className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
                  역할
                </label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value as ProjectRole)}
                  className="
                    w-full h-[36px] px-[var(--spacing-sm)]
                    border border-[var(--color-border)] rounded-[var(--radius-sm)]
                    bg-[var(--color-surface)] text-[var(--text-sm)]
                    focus:outline-none focus:border-[var(--color-primary)]
                  "
                >
                  <option value="MEMBER">MEMBER</option>
                  <option value="OWNER">OWNER</option>
                </select>
              </div>
              <div className="flex justify-end gap-[var(--spacing-sm)] pt-[var(--spacing-sm)]">
                <button
                  onClick={() => setShowInviteModal(false)}
                  className="
                    h-[32px] px-[var(--spacing-md)]
                    border border-[var(--color-border)] rounded-[var(--radius-sm)]
                    bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-secondary)]
                    cursor-pointer hover:bg-[var(--color-surface-muted)]
                  "
                >
                  취소
                </button>
                <button
                  onClick={handleInvite}
                  disabled={!inviteEmail.trim() || inviteSubmitting}
                  className="
                    h-[32px] px-[var(--spacing-md)]
                    bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
                    text-[var(--text-sm)] font-medium border-none cursor-pointer
                    hover:bg-[var(--color-primary-hover)]
                    disabled:opacity-50 disabled:cursor-not-allowed
                  "
                >
                  {inviteSubmitting ? '전송 중...' : '초대'}
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
