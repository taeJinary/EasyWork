import { beforeEach, describe, expect, it, vi } from 'vitest';
import { startOAuthLogin } from '@/oauth/oauthLogin';
import { apiOk } from '@/test/helpers';

const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    post: (...args: unknown[]) => mockPost(...args),
  },
}));

describe('oauthLogin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requests google authorize url from backend and redirects browser', async () => {
    const redirect = vi.fn();
    mockPost.mockResolvedValue(
      apiOk({
        authorizeUrl: 'https://accounts.google.com/o/oauth2/v2/auth?state=server-state',
      })
    );

    await startOAuthLogin('GOOGLE', redirect);

    expect(mockPost).toHaveBeenCalledWith('/auth/oauth/authorize-url', {
      provider: 'GOOGLE',
    });
    expect(redirect).toHaveBeenCalledWith('https://accounts.google.com/o/oauth2/v2/auth?state=server-state');
  });
});
