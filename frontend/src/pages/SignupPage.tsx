import { useState } from 'react';
import type { AxiosError } from 'axios';
import { Link, useNavigate } from 'react-router-dom';
import apiClient from '@/api/client';
import type { ApiErrorResponse, ApiResponse, SignupResponse } from '@/types';

export default function SignupPage() {
  const [nickname, setNickname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [verificationError, setVerificationError] = useState('');
  const [verificationInfo, setVerificationInfo] = useState('');
  const [verificationNotice, setVerificationNotice] = useState<SignupResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await apiClient.post<ApiResponse<SignupResponse>>('/auth/signup', {
        nickname,
        email,
        password,
      });

      const signup = response.data.data;
      if (signup.emailVerificationRequired) {
        setVerificationError('');
        setVerificationInfo('');
        setVerificationNotice(signup);
        return;
      }

      navigate('/login');
    } catch {
      setError('회원가입에 실패했습니다. 입력 정보를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendVerification = async () => {
    if (!verificationNotice) {
      return;
    }

    setVerificationError('');
    setVerificationInfo('');
    setResending(true);

    try {
      const response = await apiClient.post<ApiResponse<void>>('/auth/email-verification/resend', {
        email: verificationNotice.email,
      });
      setVerificationInfo(response.data.message ?? '인증 메일을 다시 보냈습니다.');
    } catch (caughtError) {
      const axiosError = caughtError as AxiosError<ApiErrorResponse>;
      setVerificationError(
        axiosError.response?.data?.message ?? '인증 메일 재발송에 실패했습니다.'
      );
    } finally {
      setResending(false);
    }
  };

  if (verificationNotice) {
    return (
      <div className="w-full max-w-[440px]">
        <div
          className="
            rounded-[var(--radius-md)] border border-[var(--color-border)]
            bg-[var(--color-surface)] p-[var(--spacing-lg)]
          "
        >
          <div className="text-center">
            <h1 className="m-0 text-[var(--text-xl)] font-bold text-[var(--color-text-primary)]">
              이메일을 확인해주세요
            </h1>
            <p className="mt-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
              {verificationNotice.email} 주소로 인증 링크를 보냈습니다. 이메일 인증을 완료한 뒤 로그인하세요.
            </p>
            <p className="mt-[var(--spacing-xs)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
              메일이 보이지 않으면 스팸함을 확인한 뒤 다시 보내기를 시도하세요.
            </p>
          </div>

          {verificationError && (
            <div
              className="
                mt-[var(--spacing-base)] rounded-[var(--radius-sm)] border border-[var(--color-danger)]/20
                bg-[var(--color-accent-red)] p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-danger)]
              "
            >
              {verificationError}
            </div>
          )}

          {verificationInfo && (
            <div
              className="
                mt-[var(--spacing-base)] rounded-[var(--radius-sm)] border border-[var(--color-success)]/20
                bg-[var(--color-accent-green)]/15 p-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-success)]
              "
            >
              {verificationInfo}
            </div>
          )}

          <div className="mt-[var(--spacing-lg)] flex flex-col gap-[var(--spacing-sm)] text-center">
            <button
              type="button"
              disabled={resending}
              onClick={handleResendVerification}
              className="
                h-[36px] w-full rounded-[var(--radius-sm)] border border-[var(--color-border)]
                bg-[var(--color-surface)] text-[var(--text-sm)] text-[var(--color-text-primary)]
                hover:bg-[var(--color-surface-muted)] disabled:cursor-not-allowed disabled:opacity-50
              "
            >
              {resending ? '전송 중...' : '인증 메일 다시 보내기'}
            </button>
            <Link to="/login" className="font-medium text-[var(--color-primary)]">
              로그인으로 돌아가기
            </Link>
          </div>
        </div>
      </div>
    );
  }

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
            회원가입
          </h1>
          <p className="m-0 mt-[var(--spacing-xs)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
            EasyWork에 가입하고 팀 협업을 시작하세요.
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
              닉네임
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
              이메일
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
              비밀번호
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
            {loading ? '가입 중...' : '가입하기'}
          </button>
        </form>
      </div>

      <div
        className="
          mt-[var(--spacing-base)] rounded-[var(--radius-md)] border border-[var(--color-border)]
          bg-[var(--color-surface)] p-[var(--spacing-base)] text-center text-[var(--text-sm)] text-[var(--color-text-secondary)]
        "
      >
        이미 계정이 있으신가요?{' '}
        <Link to="/login" className="font-medium text-[var(--color-primary)]">
          로그인
        </Link>
      </div>
    </div>
  );
}

