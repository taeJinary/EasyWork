import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OAuthCallbackPage from '@/pages/OAuthCallbackPage';
import { resetOAuthCodeLoginRequestCacheForTest } from '@/oauth/oauthCodeLoginRequestCache';
import { resetOAuthStateCacheForTest } from '@/oauth/oauthLogin';
import { apiOk } from '@/test/helpers';

const mockPost = vi.fn();
const mockLogin = vi.fn();
const mockNavigate = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    post: (...args: unknown[]) => mockPost(...args),
  },
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    login: mockLogin,
  }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderPage(initialEntry: string, provider: 'GOOGLE' | 'NAVER') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/oauth/:provider/callback" element={<OAuthCallbackPage provider={provider} />} />
      </Routes>
    </MemoryRouter>
  );
}

function renderPageInStrictMode(initialEntry: string, provider: 'GOOGLE' | 'NAVER') {
  return render(
    <StrictMode>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/oauth/:provider/callback" element={<OAuthCallbackPage provider={provider} />} />
        </Routes>
      </MemoryRouter>
    </StrictMode>
  );
}

describe('OAuthCallbackPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetOAuthStateCacheForTest();
    resetOAuthCodeLoginRequestCacheForTest();
    window.sessionStorage.clear();
  });

  it('logs in with google authorization code and redirects to workspaces', async () => {
    window.sessionStorage.setItem('easywork.oauth.state.GOOGLE', 'google-state');
    mockPost.mockResolvedValue(
      apiOk({
        accessToken: 'oauth-token',
        expiresIn: 3600,
        user: {
          userId: 1,
          email: 'user@example.com',
          nickname: 'tester',
          profileImg: null,
          role: 'USER',
        },
      })
    );

    renderPage('/oauth/google/callback?code=google-code&state=google-state', 'GOOGLE');

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/auth/oauth/code/login', {
        provider: 'GOOGLE',
        authorizationCode: 'google-code',
        codeVerifier: null,
        state: 'google-state',
      });
    });

    expect(mockLogin).toHaveBeenCalledWith('oauth-token', {
      userId: 1,
      email: 'user@example.com',
      nickname: 'tester',
      profileImg: null,
      role: 'USER',
    });
    expect(mockNavigate).toHaveBeenCalledWith('/workspaces', { replace: true });
  });

  it('shows error when returned state does not match expected naver state', async () => {
    window.sessionStorage.setItem('easywork.oauth.state.NAVER', 'expected-state');

    renderPage('/oauth/naver/callback?code=naver-code&state=wrong-state', 'NAVER');

    expect(await screen.findByText('소셜 로그인 실패')).toBeInTheDocument();
    expect(screen.getByText('로그인 요청 상태가 올바르지 않습니다. 다시 시도하세요.')).toBeInTheDocument();
    expect(mockPost).not.toHaveBeenCalled();
  });

  it('shows provider error when oauth provider returns access denial', async () => {
    window.sessionStorage.setItem('easywork.oauth.state.GOOGLE', 'google-state');

    renderPage('/oauth/google/callback?error=access_denied&state=google-state', 'GOOGLE');

    expect(await screen.findByText('소셜 로그인 실패')).toBeInTheDocument();
    expect(screen.getByText('소셜 로그인 제공자가 요청을 거부했습니다. 다시 시도하세요.')).toBeInTheDocument();
    expect(mockPost).not.toHaveBeenCalled();
  });

  it('does not issue duplicate oauth code login requests in StrictMode', async () => {
    window.sessionStorage.setItem('easywork.oauth.state.GOOGLE', 'google-state');
    mockPost.mockResolvedValue(
      apiOk({
        accessToken: 'oauth-token',
        expiresIn: 3600,
        user: {
          userId: 1,
          email: 'user@example.com',
          nickname: 'tester',
          profileImg: null,
          role: 'USER',
        },
      })
    );

    renderPageInStrictMode('/oauth/google/callback?code=google-code&state=google-state', 'GOOGLE');

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledTimes(1);
    });
  });
});
