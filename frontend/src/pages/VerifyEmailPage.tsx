import { useEffect, useMemo, useState } from 'react';
import type { AxiosError } from 'axios';
import { Link, useSearchParams } from 'react-router-dom';
import type { ApiErrorResponse } from '@/types';
import { requestEmailVerification } from '@/pages/verifyEmailRequestCache';

type VerifyState = 'loading' | 'success' | 'error';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const [state, setState] = useState<VerifyState>('loading');
  const [message, setMessage] = useState('인증 링크를 확인하는 중입니다.');
  const token = useMemo(() => searchParams.get('token')?.trim() ?? '', [searchParams]);

  useEffect(() => {
    let cancelled = false;

    const verifyEmail = async () => {
      if (!token) {
        setState('error');
        setMessage('유효한 인증 링크가 아닙니다.');
        return;
      }

      try {
        const result = await requestEmailVerification(token);
        if (!cancelled) {
          setState('success');
          setMessage(result.message);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        const axiosError = error as AxiosError<ApiErrorResponse>;
        setState('error');
        setMessage(axiosError.response?.data?.message ?? '이메일 인증에 실패했습니다.');
      }
    };

    void verifyEmail();

    return () => {
      cancelled = true;
    };
  }, [token]);

  const title = state === 'loading'
    ? 'Verifying your email'
    : state === 'success'
      ? 'Email verified'
      : 'Verification failed';

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
            {title}
          </h1>
          <p className="mt-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
            {message}
          </p>
        </div>

        <div className="mt-[var(--spacing-lg)] text-center">
          <Link to="/login" className="font-medium text-[var(--color-primary)]">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
