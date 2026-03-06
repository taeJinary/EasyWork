import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import apiClient from '@/api/client';
import type { ApiResponse, LoginResponse } from '@/types';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', {
        email,
        password,
      });
      const { accessToken, user } = res.data.data;
      login(accessToken, user);
      navigate('/workspaces');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-[440px]">
      <div className="
        bg-[var(--color-surface)] border border-[var(--color-border)]
        rounded-[var(--radius-md)] p-[var(--spacing-lg)]
      ">
        {/* Header */}
        <div className="text-center mb-[var(--spacing-lg)]">
          <h1 className="text-[var(--text-xl)] font-bold text-[var(--color-text-primary)] m-0">
            EasyWork
          </h1>
          <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] m-0">
            팀 협업을 위한 개발자 친화적 작업도구
          </p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit}>
          {error && (
            <div className="
              mb-[var(--spacing-base)] p-[var(--spacing-sm)]
              bg-[var(--color-accent-red)] text-[var(--color-danger)]
              text-[var(--text-sm)] rounded-[var(--radius-sm)]
              border border-[var(--color-danger)]/20
            ">
              {error}
            </div>
          )}

          <div className="mb-[var(--spacing-base)]">
            <label className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="
                w-full h-[36px] px-[var(--spacing-md)]
                border border-[var(--color-border)] rounded-[var(--radius-sm)]
                bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
              "
            />
          </div>

          <div className="mb-[var(--spacing-base)]">
            <label className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="
                w-full h-[36px] px-[var(--spacing-md)]
                border border-[var(--color-border)] rounded-[var(--radius-sm)]
                bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                focus:outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]
              "
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="
              w-full h-[36px] bg-[var(--color-primary)] text-white
              rounded-[var(--radius-sm)] text-[var(--text-sm)] font-medium
              border-none cursor-pointer
              hover:bg-[var(--color-primary-hover)]
              disabled:opacity-50 disabled:cursor-not-allowed
            "
          >
            {loading ? '로그인 중...' : 'Sign in'}
          </button>
        </form>

        {/* Divider */}
        <div className="flex items-center gap-[var(--spacing-md)] my-[var(--spacing-lg)]">
          <div className="flex-1 h-px bg-[var(--color-border)]" />
          <span className="text-[var(--text-xs)] text-[var(--color-text-muted)]">OR</span>
          <div className="flex-1 h-px bg-[var(--color-border)]" />
        </div>

        {/* OAuth Buttons */}
        <div className="flex flex-col gap-[var(--spacing-sm)]">
          <button className="
            w-full h-[36px] border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            cursor-pointer hover:bg-[var(--color-surface-muted)]
          ">
            Continue with Google
          </button>
          <button className="
            w-full h-[36px] border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            cursor-pointer hover:bg-[var(--color-surface-muted)]
          ">
            Continue with Kakao
          </button>
          <button className="
            w-full h-[36px] border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            cursor-pointer hover:bg-[var(--color-surface-muted)]
          ">
            Continue with Naver
          </button>
        </div>
      </div>

      {/* Sign up link */}
      <div className="
        mt-[var(--spacing-base)] text-center
        text-[var(--text-sm)] text-[var(--color-text-secondary)]
        bg-[var(--color-surface)] border border-[var(--color-border)]
        rounded-[var(--radius-md)] p-[var(--spacing-base)]
      ">
        계정이 없으신가요?{' '}
        <Link to="/signup" className="text-[var(--color-primary)] font-medium">
          Sign up
        </Link>
      </div>
    </div>
  );
}
