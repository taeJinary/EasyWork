import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import VerifyEmailPage from '@/pages/VerifyEmailPage';
import { apiOk } from '@/test/helpers';

const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    post: (...args: unknown[]) => mockPost(...args),
  },
}));

function renderPage(initialEntry: string) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/verify-email" element={<VerifyEmailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('verifies email with token from query string', async () => {
    mockPost.mockResolvedValue(apiOk(undefined, '이메일 인증이 완료되었습니다.'));

    renderPage('/verify-email?token=raw-token');

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/auth/email-verification/verify', {
        token: 'raw-token',
      });
    });

    expect(await screen.findByText('Email verified')).toBeInTheDocument();
    expect(screen.getByText('이메일 인증이 완료되었습니다.')).toBeInTheDocument();
  });

  it('shows error when token is missing', async () => {
    renderPage('/verify-email');

    expect(await screen.findByText('Verification failed')).toBeInTheDocument();
    expect(screen.getByText('유효한 인증 링크가 아닙니다.')).toBeInTheDocument();
    expect(mockPost).not.toHaveBeenCalled();
  });
});
