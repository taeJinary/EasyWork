import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import apiClient from '@/api/client';
import type { ApiResponse, SignupResponse } from '@/types';

export default function SignupPage() {
  const [nickname, setNickname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await apiClient.post<ApiResponse<SignupResponse>>('/auth/signup', {
        nickname,
        email,
        password,
      });
      navigate('/login');
    } catch {
      setError('회원가입에 실패했습니다. 입력 정보를 확인해주세요.');
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
            Create your account
          </h1>
          <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] m-0">
            EasyWork에 가입하여 팀 협업을 시작하세요
          </p>
        </div>

        {/* Signup Form */}
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
            <label
              htmlFor="nickname"
              className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]"
            >
              Nickname
            </label>
            <input
              id="nickname"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
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
              minLength={8}
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
            {loading ? '가입 중...' : 'Create account'}
          </button>
        </form>
      </div>

      {/* Login link */}
      <div className="
        mt-[var(--spacing-base)] text-center
        text-[var(--text-sm)] text-[var(--color-text-secondary)]
        bg-[var(--color-surface)] border border-[var(--color-border)]
        rounded-[var(--radius-md)] p-[var(--spacing-base)]
      ">
        이미 계정이 있으신가요?{' '}
        <Link to="/login" className="text-[var(--color-primary)] font-medium">
          Sign in
        </Link>
      </div>
    </div>
  );
}
