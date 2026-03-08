import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import GlobalHeader from '@/components/GlobalHeader';

const mockLogout = vi.fn();
const mockToggleMobileSidebar = vi.fn();

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: {
      userId: 1,
      email: 'nick@example.com',
      nickname: 'Nick',
      profileImg: null,
      role: 'USER',
    },
    logout: mockLogout,
  }),
}));

vi.mock('@/stores/uiStore', () => ({
  useUiStore: () => ({
    toggleMobileSidebar: mockToggleMobileSidebar,
  }),
}));

describe('GlobalHeader', () => {
  it('renders backend auth user nickname in profile controls', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <GlobalHeader />
      </MemoryRouter>
    );

    const avatarButton = screen.getByRole('button', { name: 'N' });
    expect(avatarButton).toBeInTheDocument();

    await user.click(avatarButton);

    expect(screen.getByText('Nick')).toBeInTheDocument();
    expect(screen.getByText('nick@example.com')).toBeInTheDocument();
  });
});
