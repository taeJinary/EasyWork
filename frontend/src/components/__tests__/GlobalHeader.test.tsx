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
        id: 1,
        email: 'demo@easywork.local',
        name: 'Demo',
        createdAt: '2026-03-08T00:00:00',
      },
    });
    useUiStore.setState({ isMobileSidebarOpen: false });
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
