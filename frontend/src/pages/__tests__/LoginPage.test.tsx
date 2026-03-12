import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginPage from '@/pages/LoginPage';
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

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows resend action when email is not verified', async () => {
    mockPost.mockRejectedValueOnce({
      response: {
        data: {
          errorCode: 'EMAIL_NOT_VERIFIED',
          message: '이메일 인증이 필요합니다.',
        },
      },
    });
    mockPost.mockResolvedValueOnce(apiOk(undefined, '인증 메일을 다시 보냈습니다.'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Email'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'Password1!');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByText('이메일 인증이 필요합니다. 인증 메일을 확인하거나 다시 보내세요.')).toBeInTheDocument();
    expect(
      screen.getByText('인증 메일이 오지 않았다면 스팸함을 확인한 뒤 다시 보내기를 시도하세요.')
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '인증 메일 다시 보내기' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenNthCalledWith(2, '/auth/email-verification/resend', {
        email: 'user@example.com',
      });
    });

    expect(await screen.findByText('인증 메일을 다시 보냈습니다.')).toBeInTheDocument();
  });
});
