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
      setError('Failed to sign up. Please check your input.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-[440px]">
      <div
        className="
          rounded-[var(--radius-md)] border border-[var(--color-border)]
          bg-[var(--color-surface)] p-[var(--spacing-lg)]
        "
      >
        <div className="mb-[var(--spacing-lg)] text-center">
          <h1 className="m-0 text-[var(--text-xl)] font-bold text-[var(--color-text-primary)]">
            Create your account
          </h1>
          <p className="m-0 mt-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
            Join EasyWork and start collaborating with your team.
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          {error && (
            <div
              className="
                mb-[var(--spacing-base)] rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20
                bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]
              "
            >
              {error}
            </div>
          )}

          <div className="mb-[var(--spacing-base)]">
            <label
              htmlFor="signup-nickname"
              className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
            >
              Nickname
            </label>
            <input
              id="signup-nickname"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              required
              className="
                h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
                bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
              "
            />
          </div>

          <div className="mb-[var(--spacing-base)]">
            <label
              htmlFor="signup-email"
              className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
            >
              Email
            </label>
            <input
              id="signup-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="
                h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
                bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
              "
            />
          </div>

          <div className="mb-[var(--spacing-base)]">
            <label
              htmlFor="signup-password"
              className="mb-[var(--spacing-xs)] block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)]"
            >
              Password
            </label>
            <input
              id="signup-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              className="
                h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
                bg-[var(--color-surface)] px-[var(--spacing-md)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]
              "
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="
              h-[36px] w-full rounded-[var(--radius-sm)] border-none bg-[var(--color-primary)]
              text-[var(--text-sm)] font-medium text-white hover:bg-[var(--color-primary-hover)]
              disabled:cursor-not-allowed disabled:opacity-50
            "
          >
            {loading ? 'Creating...' : 'Create account'}
          </button>
        </form>
      </div>

      <div
        className="
          mt-[var(--spacing-base)] rounded-[var(--radius-md)] border border-[var(--color-border)]
          bg-[var(--color-surface)] p-[var(--spacing-base)] text-center text-[var(--text-sm)] text-[var(--color-text-secondary)]
        "
      >
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-[var(--color-primary)]">
          Sign in
        </Link>
      </div>
    </div>
  );
}
