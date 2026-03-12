import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SignupPage from '@/pages/SignupPage';
import { apiOk } from '@/test/helpers';

const mockPost = vi.fn();
const mockNavigate = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    post: (...args: unknown[]) => mockPost(...args),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('SignupPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits nickname field required by backend signup contract', async () => {
    mockPost.mockResolvedValue(
      apiOk({ userId: 1, email: 'nick@example.com', nickname: 'Nick', emailVerificationRequired: true })
    );

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    const nicknameInput = screen.getByLabelText('닉네임');
    const emailInput = screen.getByLabelText('이메일');
    const passwordInput = screen.getByLabelText('비밀번호');

    fireEvent.change(nicknameInput, { target: { value: 'Nick' } });
    fireEvent.change(emailInput, { target: { value: 'nick@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'Password1!' } });
    fireEvent.click(screen.getByRole('button', { name: '가입하기' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/auth/signup', {
        nickname: 'Nick',
        email: 'nick@example.com',
        password: 'Password1!',
      });
    });
  });

  it('shows verification notice when signup requires email verification', async () => {
    mockPost.mockResolvedValue(
      apiOk({ userId: 1, email: 'nick@example.com', nickname: 'Nick', emailVerificationRequired: true })
    );

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('닉네임'), 'Nick');
    await user.type(screen.getByLabelText('이메일'), 'nick@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'Password1!');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    expect(await screen.findByText('이메일을 확인해주세요')).toBeInTheDocument();
    expect(screen.getByText(/nick@example.com 주소로 인증 링크를 보냈습니다/)).toBeInTheDocument();
    expect(
      screen.getByText('메일이 보이지 않으면 스팸함을 확인한 뒤 다시 보내기를 시도하세요.')
    ).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('resends verification email from signup notice', async () => {
    mockPost.mockResolvedValueOnce(
      apiOk({ userId: 1, email: 'nick@example.com', nickname: 'Nick', emailVerificationRequired: true })
    );
    mockPost.mockResolvedValueOnce(apiOk(undefined, '인증 메일을 다시 보냈습니다.'));

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('닉네임'), 'Nick');
    await user.type(screen.getByLabelText('이메일'), 'nick@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'Password1!');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    expect(await screen.findByText('이메일을 확인해주세요')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '인증 메일 다시 보내기' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenNthCalledWith(2, '/auth/email-verification/resend', {
        email: 'nick@example.com',
      });
    });

    expect(await screen.findByText('인증 메일을 다시 보냈습니다.')).toBeInTheDocument();
  });

  it('shows backend resend message when verification resend is throttled', async () => {
    mockPost.mockResolvedValueOnce(
      apiOk({ userId: 1, email: 'nick@example.com', nickname: 'Nick', emailVerificationRequired: true })
    );
    mockPost.mockRejectedValueOnce({
      response: {
        data: {
          errorCode: 'EMAIL_VERIFICATION_RESEND_TOO_FREQUENT',
          message: '인증 메일을 다시 보내기까지 잠시 기다려주세요.',
        },
      },
    });

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('닉네임'), 'Nick');
    await user.type(screen.getByLabelText('이메일'), 'nick@example.com');
    await user.type(screen.getByLabelText('비밀번호'), 'Password1!');
    await user.click(screen.getByRole('button', { name: '가입하기' }));

    expect(await screen.findByText('이메일을 확인해주세요')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '인증 메일 다시 보내기' }));

    expect(await screen.findByText('인증 메일을 다시 보내기까지 잠시 기다려주세요.')).toBeInTheDocument();
  });
});
