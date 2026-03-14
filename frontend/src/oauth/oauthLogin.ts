export type OAuthProvider = 'GOOGLE' | 'NAVER';

const OAUTH_STATE_STORAGE_PREFIX = 'easywork.oauth.state.';
const GOOGLE_AUTHORIZE_URL = 'https://accounts.google.com/o/oauth2/v2/auth';
const NAVER_AUTHORIZE_URL = 'https://nid.naver.com/oauth2.0/authorize';
const GOOGLE_SCOPE = 'openid email profile';
const consumedOAuthStateCache = new Map<OAuthProvider, string | null>();

function getClientId(provider: OAuthProvider) {
  if (provider === 'GOOGLE') {
    return (import.meta.env.VITE_OAUTH_GOOGLE_CLIENT_ID?.trim() || __OAUTH_GOOGLE_CLIENT_ID__?.trim() || '');
  }

  return (import.meta.env.VITE_OAUTH_NAVER_CLIENT_ID?.trim() || __OAUTH_NAVER_CLIENT_ID__?.trim() || '');
}

function getRedirectUri(provider: OAuthProvider) {
  if (provider === 'GOOGLE') {
    return (
      import.meta.env.VITE_OAUTH_GOOGLE_REDIRECT_URI?.trim()
      || __OAUTH_GOOGLE_REDIRECT_URI__?.trim()
      || `${window.location.origin}/oauth/google/callback`
    );
  }

  return (
    import.meta.env.VITE_OAUTH_NAVER_REDIRECT_URI?.trim()
    || __OAUTH_NAVER_REDIRECT_URI__?.trim()
    || `${window.location.origin}/oauth/naver/callback`
  );
}

function createState() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function getStateStorageKey(provider: OAuthProvider) {
  return `${OAUTH_STATE_STORAGE_PREFIX}${provider}`;
}

export function consumeOAuthState(provider: OAuthProvider) {
  if (consumedOAuthStateCache.has(provider)) {
    return consumedOAuthStateCache.get(provider) ?? null;
  }

  const storageKey = getStateStorageKey(provider);
  const state = window.sessionStorage.getItem(storageKey);
  window.sessionStorage.removeItem(storageKey);
  consumedOAuthStateCache.set(provider, state);
  return state;
}

export function buildOAuthAuthorizeUrl(provider: OAuthProvider, state: string) {
  const clientId = getClientId(provider);
  if (!clientId) {
    throw new Error(`${provider} OAuth client id is not configured.`);
  }

  const url = new URL(provider === 'GOOGLE' ? GOOGLE_AUTHORIZE_URL : NAVER_AUTHORIZE_URL);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', clientId);
  url.searchParams.set('redirect_uri', getRedirectUri(provider));
  url.searchParams.set('state', state);

  if (provider === 'GOOGLE') {
    url.searchParams.set('scope', GOOGLE_SCOPE);
  }

  return url.toString();
}

export function startOAuthLogin(
  provider: OAuthProvider,
  redirect: (url: string) => void = (url) => window.location.assign(url)
) {
  const state = createState();
  consumedOAuthStateCache.delete(provider);
  window.sessionStorage.setItem(getStateStorageKey(provider), state);
  const authorizeUrl = buildOAuthAuthorizeUrl(provider, state);
  redirect(authorizeUrl);
}

export function resetOAuthStateCacheForTest() {
  consumedOAuthStateCache.clear();
}
