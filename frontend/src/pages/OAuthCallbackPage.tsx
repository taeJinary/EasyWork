import { useEffect, useMemo, useState } from 'react';
import type { AxiosError } from 'axios';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import type { OAuthProvider } from '@/oauth/oauthLogin';
import { requestOAuthCodeLogin } from '@/oauth/oauthCodeLoginRequestCache';
import type { ApiErrorResponse } from '@/types';

type OAuthCallbackPageProps = {
  provider: OAuthProvider;
};

type OAuthCallbackState = 'loading' | 'error';

function getProviderLabel(provider: OAuthProvider) {
  return provider === 'GOOGLE' ? 'Google' : '네이버';
}

export default function OAuthCallbackPage({ provider }: OAuthCallbackPageProps) {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [pageState, setPageState] = useState<OAuthCallbackState>('loading');
  const [message, setMessage] = useState('소셜 로그인 응답을 확인하는 중입니다.');
  const providerLabel = getProviderLabel(provider);
  const authorizationCode = useMemo(() => searchParams.get('code')?.trim() ?? '', [searchParams]);
  const state = useMemo(() => searchParams.get('state')?.trim() ?? '', [searchParams]);
  const providerError = useMemo(() => searchParams.get('error')?.trim() ?? '', [searchParams]);

  useEffect(() => {
    let cancelled = false;

    const completeOAuthLogin = async () => {
      if (providerError) {
        setPageState('error');
        setMessage('소셜 로그인 제공자가 요청을 거부했습니다. 다시 시도하세요.');
        return;
      }

      if (!authorizationCode) {
        setPageState('error');
        setMessage('유효한 소셜 로그인 응답이 아닙니다. 다시 시도하세요.');
        return;
      }

      if (!state) {
        setPageState('error');
        setMessage('로그인 요청 상태가 올바르지 않습니다. 다시 시도하세요.');
        return;
      }

      try {
        const response = await requestOAuthCodeLogin({
          provider,
          authorizationCode,
          codeVerifier: null,
          state,
        });

        if (cancelled) {
          return;
        }

        const { accessToken, user } = response;
        login(accessToken, user);
        navigate('/dashboard', { replace: true });
      } catch (error) {
        if (cancelled) {
          return;
        }

        const axiosError = error as AxiosError<ApiErrorResponse>;
        setPageState('error');
        setMessage(axiosError.response?.data?.message ?? `${providerLabel} 로그인에 실패했습니다.`);
      }
    };

    void completeOAuthLogin();

    return () => {
      cancelled = true;
    };
  }, [authorizationCode, login, navigate, provider, providerError, providerLabel, state]);

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
            {pageState === 'loading' ? `${providerLabel} 로그인 중` : '소셜 로그인 실패'}
          </h1>
          <p className="mt-[var(--spacing-sm)] text-[var(--text-sm)] text-[var(--color-text-secondary)]">
            {message}
          </p>
          {pageState === 'error' && (
            <p className="mt-[var(--spacing-xs)] text-[var(--text-xs)] text-[var(--color-text-muted)]">
              로그인 화면으로 돌아가 다시 시도하세요.
            </p>
          )}
        </div>

        {pageState === 'error' && (
          <div className="mt-[var(--spacing-lg)] text-center">
            <Link to="/login" className="font-medium text-[var(--color-primary)]">
              로그인으로 이동
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
