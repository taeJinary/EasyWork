import { beforeEach, describe, expect, it, vi } from 'vitest';
import { buildOAuthAuthorizeUrl, consumeOAuthState, startOAuthLogin } from '@/oauth/oauthLogin';

describe('oauthLogin', () => {
  beforeEach(() => {
    vi.unstubAllEnvs();
    window.sessionStorage.clear();
    window.history.replaceState({}, '', '/login');
  });

  it('builds google authorize url with configured client id and callback uri', () => {
    vi.stubEnv('VITE_OAUTH_GOOGLE_CLIENT_ID', 'google-client-id');
    vi.stubEnv('VITE_OAUTH_GOOGLE_REDIRECT_URI', 'http://localhost:5173/oauth/google/callback');

    const authorizeUrl = buildOAuthAuthorizeUrl('GOOGLE', 'google-state');
    const url = new URL(authorizeUrl);

    expect(url.origin + url.pathname).toBe('https://accounts.google.com/o/oauth2/v2/auth');
    expect(url.searchParams.get('client_id')).toBe('google-client-id');
    expect(url.searchParams.get('redirect_uri')).toBe('http://localhost:5173/oauth/google/callback');
    expect(url.searchParams.get('state')).toBe('google-state');
    expect(url.searchParams.get('scope')).toBe('openid email profile');
  });

  it('stores state and redirects when starting naver oauth login', () => {
    vi.stubEnv('VITE_OAUTH_NAVER_CLIENT_ID', 'naver-client-id');
    vi.stubEnv('VITE_OAUTH_NAVER_REDIRECT_URI', 'http://localhost:5173/oauth/naver/callback');
    const redirect = vi.fn();

    startOAuthLogin('NAVER', redirect);

    expect(redirect).toHaveBeenCalledTimes(1);
    const redirectedUrl = new URL(redirect.mock.calls[0][0] as string);
    expect(redirectedUrl.origin + redirectedUrl.pathname).toBe('https://nid.naver.com/oauth2.0/authorize');
    expect(redirectedUrl.searchParams.get('client_id')).toBe('naver-client-id');
    expect(redirectedUrl.searchParams.get('redirect_uri')).toBe('http://localhost:5173/oauth/naver/callback');

    const state = consumeOAuthState('NAVER');
    expect(state).toBeTruthy();
    expect(redirectedUrl.searchParams.get('state')).toBe(state);
  });
});
