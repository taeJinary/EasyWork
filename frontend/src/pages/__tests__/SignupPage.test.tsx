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
    mockPost.mockResolvedValue(apiOk({ userId: 1, email: 'tester@example.com', nickname: 'tester' }));

    const { container } = render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    const textInputs = screen.getAllByRole('textbox');
    const passwordInput = container.querySelector('input[type="password"]');

    expect(textInputs).toHaveLength(2);
    expect(passwordInput).not.toBeNull();

    await user.type(textInputs[0], 'tester');
    await user.type(textInputs[1], 'tester@example.com');
    await user.type(passwordInput!, 'Abcd1234!');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/auth/signup', {
        nickname: 'tester',
        email: 'tester@example.com',
        password: 'Abcd1234!',
      });
    });
  });
});
