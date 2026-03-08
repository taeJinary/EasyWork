import { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle } from 'lucide-react';
import PageHeader from '@/components/PageHeader';
import apiClient from '@/api/client';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse, UserProfile } from '@/types';

export default function ProfileSettingsPage() {
  const { setUser, user } = useAuthStore();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [nickname, setNickname] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchProfile() {
      try {
        setLoading(true);
        setError(null);
        const res = await apiClient.get<ApiResponse<UserProfile>>('/users/me');
        setProfile(res.data.data);
        setNickname(res.data.data.nickname);
      } catch (err) {
        setError('프로필을 불러오는 데 실패했습니다.');
        console.error('Failed to fetch profile:', err);
      } finally {
        setLoading(false);
      }
    }
    fetchProfile();
  }, []);

  // Client-side validation
  const validateNickname = (value: string): string | null => {
    if (value.length < 2) return '닉네임은 2자 이상이어야 합니다.';
    if (value.length > 20) return '닉네임은 20자 이하여야 합니다.';
    return null;
  };

  const handleNicknameChange = (value: string) => {
    setNickname(value);
    setSuccess(null);
    const err = value.trim() ? validateNickname(value) : null;
    setValidationError(err);
  };

  const canSave = nickname.trim().length > 0 && !validationError && nickname !== profile?.nickname && !submitting;

  const handleSave = async () => {
    const err = validateNickname(nickname);
    if (err) {
      setValidationError(err);
      return;
    }
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await apiClient.patch<ApiResponse<UserProfile>>('/users/me', { nickname });
      setProfile(res.data.data);
      setNickname(res.data.data.nickname);
      setSuccess('프로필이 저장되었습니다.');
      // Sync auth store
      if (user) {
        setUser({
          ...user,
          email: res.data.data.email,
          nickname: res.data.data.nickname,
          profileImg: res.data.data.profileImg,
        });
      }
    } catch (err) {
      setError('프로필 저장에 실패했습니다.');
      console.error('Failed to update profile:', err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader title="프로필 설정" description="개인 프로필 정보를 관리합니다." />

      {/* Error */}
      {error && (
        <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)] bg-[var(--color-accent-red)] border border-[var(--color-danger)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-danger)]">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      {/* Success */}
      {success && (
        <div className="flex items-center gap-[var(--spacing-sm)] p-[var(--spacing-sm)] mt-[var(--spacing-sm)] bg-green-50 border border-[var(--color-success)] rounded-[var(--radius-sm)] text-[var(--text-sm)] text-[var(--color-success)]">
          <CheckCircle size={14} className="shrink-0" />
          {success}
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="mt-[var(--spacing-lg)] animate-pulse space-y-4">
          <div className="flex items-center gap-[var(--spacing-base)]">
            <div className="w-[72px] h-[72px] rounded-full bg-[var(--color-surface-muted)]" />
            <div className="space-y-2">
              <div className="h-4 w-32 bg-[var(--color-surface-muted)] rounded" />
              <div className="h-3 w-48 bg-[var(--color-surface-muted)] rounded" />
            </div>
          </div>
          <div className="h-10 w-full bg-[var(--color-surface-muted)] rounded" />
        </div>
      )}

      {/* Profile form */}
      {!loading && profile && (
        <div className="mt-[var(--spacing-lg)] max-w-[480px]">
          {/* Avatar row */}
          <div className="flex items-center gap-[var(--spacing-base)] mb-[var(--spacing-lg)]">
            <div className="
              w-[72px] h-[72px] rounded-full bg-[var(--color-primary)]
              text-white text-[var(--text-xl)] font-bold
              flex items-center justify-center
            ">
              {profile.nickname.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="text-[var(--text-base)] font-semibold text-[var(--color-text-primary)]">
                {profile.nickname}
              </div>
              <div className="text-[var(--text-sm)] text-[var(--color-text-muted)]">
                {profile.email}
              </div>
              <div className="text-[var(--text-xs)] text-[var(--color-text-muted)] mt-[2px]">
                {profile.provider.toUpperCase() === 'LOCAL' ? '이메일 가입' : profile.provider}
              </div>
            </div>
          </div>

          {/* Nickname field */}
          <div className="mb-[var(--spacing-base)]">
            <label htmlFor="nickname" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
              닉네임
            </label>
            <input
              id="nickname"
              type="text"
              value={nickname}
              onChange={(e) => handleNicknameChange(e.target.value)}
              maxLength={25}
              className={`
                w-full h-[36px] px-[var(--spacing-sm)]
                border rounded-[var(--radius-sm)]
                bg-[var(--color-surface)] text-[var(--text-sm)]
                focus:outline-none focus:ring-1
                ${validationError
                  ? 'border-[var(--color-danger)] focus:border-[var(--color-danger)] focus:ring-[var(--color-danger)]'
                  : 'border-[var(--color-border)] focus:border-[var(--color-primary)] focus:ring-[var(--color-primary)]'
                }
              `}
            />
            {validationError && (
              <p className="text-[var(--text-xs)] text-[var(--color-danger)] mt-[var(--spacing-xs)] m-0">
                {validationError}
              </p>
            )}
            <p className="text-[var(--text-xs)] text-[var(--color-text-muted)] mt-[var(--spacing-xs)] m-0">
              {nickname.length}/20
            </p>
          </div>

          {/* Email (read-only) */}
          <div className="mb-[var(--spacing-base)]">
            <label className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
              이메일
            </label>
            <div className="
              h-[36px] px-[var(--spacing-sm)] flex items-center
              border border-[var(--color-border)] rounded-[var(--radius-sm)]
              bg-[var(--color-surface-muted)] text-[var(--text-sm)] text-[var(--color-text-muted)]
            ">
              {profile.email}
            </div>
          </div>

          {/* Save */}
          <button
            onClick={handleSave}
            disabled={!canSave}
            className="
              h-[36px] px-[var(--spacing-lg)]
              bg-[var(--color-primary)] text-white rounded-[var(--radius-sm)]
              text-[var(--text-sm)] font-medium border-none cursor-pointer
              hover:bg-[var(--color-primary-hover)]
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            {submitting ? '저장 중...' : '저장'}
          </button>
        </div>
      )}
    </div>
  );
}
