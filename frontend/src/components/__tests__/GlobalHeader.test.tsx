import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import GlobalHeader from '@/components/GlobalHeader';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

function LocationDisplay() {
  const location = useLocation();
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
}

describe('GlobalHeader', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    mockGet.mockResolvedValue(apiOk({ unreadCount: 0 }));
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
    await user.click(screen.getByRole('button', { name: '새로 만들기' }));

    expect(screen.getByTestId('location')).toHaveTextContent('/workspaces?create=workspace');
  });

  it('renders unread notification count from backend', async () => {
    mockGet.mockResolvedValue(apiOk({ unreadCount: 3 }));

    render(
      <MemoryRouter>
        <GlobalHeader />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  it('navigates to account settings from the profile menu settings link', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <GlobalHeader />
        <LocationDisplay />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'N' }));
    await user.click(screen.getByRole('link', { name: '설정' }));

    expect(screen.getByTestId('location')).toHaveTextContent('/settings/account');
  });

  it('navigates to profile settings from the profile menu profile link', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <GlobalHeader />
        <LocationDisplay />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'N' }));
    await user.click(screen.getByRole('link', { name: '프로필' }));

    expect(screen.getByTestId('location')).toHaveTextContent('/settings/profile');
  });

  it('uses Korean search placeholder in the global search box', () => {
    render(
      <MemoryRouter>
        <GlobalHeader />
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText('검색...')).toBeInTheDocument();
  });

  it('navigates to the global search page when submitting a query', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <GlobalHeader />
        <LocationDisplay />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    const searchInput = screen.getByPlaceholderText('검색...');

    await user.type(searchInput, 'dd{enter}');

    expect(screen.getByTestId('location')).toHaveTextContent('/search?q=dd');
  });

  it('shows inline preview results under the search box while typing', async () => {
    mockGet.mockImplementation((url: string, config?: { params?: Record<string, unknown> }) => {
      if (url === '/notifications/unread-count') {
        return Promise.resolve(apiOk({ unreadCount: 0 }));
      }

      if (url === '/projects') {
        expect(config).toEqual({ params: { page: 0, size: 5, keyword: 'dd' } });
        return Promise.resolve(
          apiOk({
            content: [
              {
                projectId: 10,
                name: 'dd project',
                description: 'target project',
                role: 'OWNER',
                memberCount: 2,
                taskCount: 3,
                doneTaskCount: 1,
                progressRate: 33,
                updatedAt: '2026-03-15T09:00:00',
              },
            ],
            page: 0,
            size: 5,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
          })
        );
      }

      if (url === '/workspaces') {
        return Promise.resolve(
          apiOk({
            content: [
              {
                workspaceId: 2,
                name: 'dd workspace',
                description: 'target workspace',
                myRole: 'OWNER',
                memberCount: 4,
                updatedAt: '2026-03-15T08:00:00',
              },
            ],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
          })
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <GlobalHeader />
      </MemoryRouter>
    );

    const user = userEvent.setup();
    const searchInput = screen.getByPlaceholderText('검색...');

    await user.type(searchInput, 'dd');

    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'dd project' })).toHaveAttribute(
        'href',
        '/projects/10/board'
      );
    });

    expect(screen.getByRole('link', { name: 'dd workspace' })).toHaveAttribute(
      'href',
      '/workspaces/2'
    );
  });

  it('does not render the workspace tab button next to the brand', () => {
    render(
      <MemoryRouter>
        <GlobalHeader />
      </MemoryRouter>
    );

    expect(screen.queryByRole('button', { name: /워크스페이스/i })).not.toBeInTheDocument();
  });

  it('renders the header above sidebar overlays for the search preview layer', () => {
    render(
      <MemoryRouter>
        <GlobalHeader />
      </MemoryRouter>
    );

    expect(screen.getByRole('banner')).toHaveClass('z-[60]');
  });
});
