import apiClient from '@/api/client';
import type { ApiResponse } from '@/types';

export type VerifyResult = { message: string };

const verificationRequestCache = new Map<string, Promise<VerifyResult>>();

export function requestEmailVerification(token: string): Promise<VerifyResult> {
  const cachedRequest = verificationRequestCache.get(token);
  if (cachedRequest) {
    return cachedRequest;
  }

  const request = apiClient
    .post<ApiResponse<void>>('/auth/email-verification/verify', { token })
    .then((response) => ({
      message: response.data.message ?? '이메일 인증이 완료되었습니다.',
    }))
    .catch((error) => {
      verificationRequestCache.delete(token);
      throw error;
    });

  verificationRequestCache.set(token, request);
  return request;
}

export function resetVerifyEmailRequestCacheForTest() {
  verificationRequestCache.clear();
}
