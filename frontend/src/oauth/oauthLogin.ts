import apiClient from '@/api/client';
import type { ApiResponse } from '@/types';

export type OAuthProvider = 'GOOGLE' | 'NAVER';

type OAuthAuthorizeUrlResponse = {
  authorizeUrl: string;
};

export async function startOAuthLogin(
  provider: OAuthProvider,
  redirect: (url: string) => void = (url) => window.location.assign(url)
) {
  const response = await apiClient.post<ApiResponse<OAuthAuthorizeUrlResponse>>('/auth/oauth/authorize-url', {
    provider,
  });
  const authorizeUrl = response.data.data.authorizeUrl?.trim();

  if (!authorizeUrl) {
    throw new Error('OAuth authorize url is missing.');
  }

  redirect(authorizeUrl);
}
