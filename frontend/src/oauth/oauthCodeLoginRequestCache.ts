import apiClient from '@/api/client';
import type { ApiResponse, LoginResponse } from '@/types';
import type { OAuthProvider } from '@/oauth/oauthLogin';

type OAuthCodeLoginRequest = {
  provider: OAuthProvider;
  authorizationCode: string;
  codeVerifier: string | null;
  state: string;
};

const oauthCodeLoginRequestCache = new Map<string, Promise<LoginResponse>>();

function createCacheKey(request: OAuthCodeLoginRequest) {
  return `${request.provider}:${request.authorizationCode}:${request.state}`;
}

export function requestOAuthCodeLogin(request: OAuthCodeLoginRequest): Promise<LoginResponse> {
  const cacheKey = createCacheKey(request);
  const cachedRequest = oauthCodeLoginRequestCache.get(cacheKey);
  if (cachedRequest) {
    return cachedRequest;
  }

  const pendingRequest = apiClient
    .post<ApiResponse<LoginResponse>>('/auth/oauth/code/login', request)
    .then((response) => response.data.data)
    .catch((error) => {
      oauthCodeLoginRequestCache.delete(cacheKey);
      throw error;
    });

  oauthCodeLoginRequestCache.set(cacheKey, pendingRequest);
  return pendingRequest;
}

export function resetOAuthCodeLoginRequestCacheForTest() {
  oauthCodeLoginRequestCache.clear();
}
