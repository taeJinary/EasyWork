import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import GlobalHeader from '@/components/GlobalHeader';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';

function LocationDisplay() {
  const location = useLocation();
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
}

describe('GlobalHeader', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({
      accessToken: 'token',
      isAuthenticated: true,
      user: {
        userId: 1,
        email: 'nick@example.com',
        nickname: 'Nick',
        profileImg: null,
        role: 'USER',
      },
    });
    useUiStore.setState({ isMobileSidebarOpen: false });
  });

  it('renders backend auth user nickname in profile controls', async () => {
    render(
      <MemoryRouter>
        <GlobalHeader />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    const avatarButton = screen.getByRole('button', { name: 'N' });

    expect(avatarButton).toBeInTheDocument();

    await user.click(avatarButton);

    expect(screen.getByText('Nick')).toBeInTheDocument();
    expect(screen.getByText('nick@example.com')).toBeInTheDocument();
  });

  it('navigates to workspace creation flow when clicking New', async () => {
    render(
      <MemoryRouter initialEntries={['/projects']}>
        <GlobalHeader />
        <LocationDisplay />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /new/i }));

    expect(screen.getByTestId('location')).toHaveTextContent('/workspaces?create=workspace');
  });
});
