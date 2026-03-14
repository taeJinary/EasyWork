import { useState } from 'react';
import type { AxiosError } from 'axios';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import apiClient from '@/api/client';
import type { ApiErrorResponse, ApiResponse, LoginResponse } from '@/types';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [requiresVerification, setRequiresVerification] = useState(false);
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setInfo('');
    setRequiresVerification(false);
    setLoading(true);

    try {
      const res = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', {
        email,
        password,
      });
      const { accessToken, user } = res.data.data;
      login(accessToken, user);
      navigate('/workspaces');
    } catch (error) {
      const axiosError = error as AxiosError<ApiErrorResponse>;
      const errorCode = axiosError.response?.data?.errorCode;

      if (errorCode === 'EMAIL_NOT_VERIFIED') {
        setRequiresVerification(true);
        setError('이메일 인증이 필요합니다. 인증 메일을 확인하거나 다시 보내세요.');
      } else {
        setError('이메일 또는 비밀번호가 올바르지 않습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendVerification = async () => {
    setError('');
    setInfo('');
    setResending(true);

    try {
      const response = await apiClient.post<ApiResponse<void>>('/auth/email-verification/resend', {
        email,
      });
      setInfo(response.data.message ?? '인증 메일을 다시 보냈습니다.');
    } catch (error) {
      const axiosError = error as AxiosError<ApiErrorResponse>;
      setError(axiosError.response?.data?.message ?? '인증 메일 재발송에 실패했습니다.');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="w-full max-w-[440px]">
      <div className="
        bg-[var(--color-surface)] border border-[var(--color-border)]
        rounded-[var(--radius-md)] p-[var(--spacing-lg)]
      ">
        <div className="text-center mb-[var(--spacing-lg)]">
          <h1 className="text-[var(--text-xl)] font-bold text-[var(--color-text-primary)] m-0">
            EasyWork
          </h1>
          <p className="text-[var(--text-sm)] text-[var(--color-text-secondary)] mt-[var(--spacing-xs)] m-0">
            팀 협업을 위한 개발자 친화적 작업도구
          </p>
        </div>

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

          {info && (
            <div className="
              mb-[var(--spacing-base)] p-[var(--spacing-sm)]
              bg-[var(--color-accent-green)]/15 text-[var(--color-success)]
              text-[var(--text-sm)] rounded-[var(--radius-sm)]
              border border-[var(--color-success)]/20
            ">
              {info}
            </div>
          )}

          <div className="mb-[var(--spacing-base)]">
            <label htmlFor="login-email" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
              이메일
            </label>
            <input
              id="login-email"
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
            <label htmlFor="login-password" className="block text-[var(--text-sm)] font-medium text-[var(--color-text-primary)] mb-[var(--spacing-xs)]">
              비밀번호
            </label>
            <input
              id="login-password"
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
            {loading ? '로그인 중...' : '로그인'}
          </button>

          {requiresVerification && (
            <div className="mt-[var(--spacing-sm)]">
              <p className="mb-[var(--spacing-xs)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
                인증 메일이 오지 않았다면 스팸함을 확인한 뒤 다시 보내기를 시도하세요.
              </p>
              <button
                type="button"
                disabled={resending || !email}
                onClick={handleResendVerification}
                className="
                  w-full h-[36px] border border-[var(--color-border)] rounded-[var(--radius-sm)]
                  bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                  cursor-pointer hover:bg-[var(--color-surface-muted)] disabled:opacity-50 disabled:cursor-not-allowed
                "
              >
                {resending ? '전송 중...' : '인증 메일 다시 보내기'}
              </button>
            </div>
          )}
        </form>

        <div className="flex items-center gap-[var(--spacing-md)] my-[var(--spacing-lg)]">
          <div className="flex-1 h-px bg-[var(--color-border)]" />
          <span className="text-[var(--text-xs)] text-[var(--color-text-muted)]">또는</span>
          <div className="flex-1 h-px bg-[var(--color-border)]" />
        </div>

        <div className="flex flex-col gap-[var(--spacing-sm)]">
          <button className="
            w-full h-[36px] border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            cursor-pointer hover:bg-[var(--color-surface-muted)]
          ">
            Google로 계속하기
          </button>
          <button className="
            w-full h-[36px] border border-[var(--color-border)] rounded-[var(--radius-sm)]
            bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
            cursor-pointer hover:bg-[var(--color-surface-muted)]
          ">
            네이버로 계속하기
          </button>
        </div>
      </div>

      <div className="
        mt-[var(--spacing-base)] text-center
        text-[var(--text-sm)] text-[var(--color-text-secondary)]
        bg-[var(--color-surface)] border border-[var(--color-border)]
        rounded-[var(--radius-md)] p-[var(--spacing-base)]
      ">
        계정이 없으신가요?{' '}
        <Link to="/signup" className="text-[var(--color-primary)] font-medium">
          회원가입
        </Link>
      </div>
    </div>
  );
}
