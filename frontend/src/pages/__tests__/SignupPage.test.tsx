import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import SignupPage from '@/pages/SignupPage';
import { apiOk } from '@/test/helpers';

const mockPost = vi.fn();
const mockNavigate = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    post: (...args: unknown[]) => mockPost(...args),
    get: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
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

  it('submits nickname field using backend signup contract', async () => {
    mockPost.mockResolvedValue(
      apiOk({
        userId: 1,
        email: 'nick@example.com',
        nickname: 'Nick',
      })
    );

    const user = userEvent.setup();
    const { container } = render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    const textInputs = container.querySelectorAll('input[type="text"], input[type="email"]');
    const passwordInput = container.querySelector('input[type="password"]');

    expect(textInputs).toHaveLength(2);
    expect(passwordInput).not.toBeNull();

    await user.type(textInputs[0] as HTMLInputElement, 'Nick');
    await user.type(textInputs[1] as HTMLInputElement, 'nick@example.com');
    await user.type(passwordInput as HTMLInputElement, 'Password1!');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/auth/signup', {
        nickname: 'Nick',
        email: 'nick@example.com',
        password: 'Password1!',
      });
    });
  });
});
