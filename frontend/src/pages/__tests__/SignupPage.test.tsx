import { render, screen, waitFor } from '@testing-library/react';
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

    const user = userEvent.setup();
    const nicknameInput = screen.getByLabelText('Nickname');
    const emailInput = screen.getByLabelText('Email');
    const passwordInput = screen.getByLabelText('Password');

    await user.type(nicknameInput, 'Nick');
    await user.type(emailInput, 'nick@example.com');
    await user.type(passwordInput, 'Password1!');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

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
    await user.type(screen.getByLabelText('Nickname'), 'Nick');
    await user.type(screen.getByLabelText('Email'), 'nick@example.com');
    await user.type(screen.getByLabelText('Password'), 'Password1!');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Check your email')).toBeInTheDocument();
    expect(screen.getByText(/We sent a verification link to nick@example.com/)).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
