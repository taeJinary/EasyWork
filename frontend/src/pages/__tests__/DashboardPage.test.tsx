import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '@/App';
import { useAuthStore } from '@/stores/authStore';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('Dashboard route', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
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
  });

  it('renders dashboard projects summary and selected project stats on /dashboard', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/dashboard/projects') {
        return Promise.resolve(
          apiOk({
            pendingInvitationCount: 2,
            myProjects: [
              {
                projectId: 11,
                name: 'Alpha Project',
                role: 'OWNER',
                memberCount: 4,
                taskCount: 12,
                doneTaskCount: 5,
                progressRate: 42,
                updatedAt: '2026-03-08T10:00:00',
              },
              {
                projectId: 12,
                name: 'Beta Project',
                role: 'MEMBER',
                memberCount: 6,
                taskCount: 9,
                doneTaskCount: 2,
                progressRate: 22,
                updatedAt: '2026-03-08T11:00:00',
              },
            ],
          })
        );
      }

      if (url === '/projects/11/dashboard') {
        return Promise.resolve(
          apiOk({
            projectId: 11,
            memberCount: 4,
            taskCount: 12,
            todoCount: 3,
            inProgressCount: 4,
            doneCount: 5,
            overdueCount: 1,
            dueSoonCount: 2,
            completionRate: 42,
          })
        );
      }

      if (url === '/projects/12/dashboard') {
        return Promise.resolve(
          apiOk({
            projectId: 12,
            memberCount: 6,
            taskCount: 9,
            todoCount: 5,
            inProgressCount: 2,
            doneCount: 2,
            overdueCount: 0,
            dueSoonCount: 3,
            completionRate: 22,
          })
        );
      }

      if (url === '/notifications/unread-count') {
        return Promise.resolve(apiOk({ unreadCount: 3 }));
      }

      return Promise.reject(new Error(`unexpected url: ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <App />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    });

    expect(screen.getAllByText('Alpha Project').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Beta Project').length).toBeGreaterThan(0);
    expect(screen.getByText('42% complete')).toBeInTheDocument();
    expect(screen.getByText('Project Stats')).toBeInTheDocument();
    expect(await screen.findByText('Overdue')).toBeInTheDocument();
    expect(screen.getByText('Due Soon')).toBeInTheDocument();

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/projects/11/dashboard');
    });

    const user = userEvent.setup();
    const viewStatsButtons = screen.getAllByRole('button', { name: 'View Stats' });
    await user.click(viewStatsButtons[1]);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/projects/12/dashboard');
    });

    expect(screen.getAllByText('Beta Project').length).toBeGreaterThan(0);
    expect(screen.getByText('22%')).toBeInTheDocument();
  });

  it('shows an inline error when project stats fail to load', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/dashboard/projects') {
        return Promise.resolve(
          apiOk({
            pendingInvitationCount: 1,
            myProjects: [
              {
                projectId: 11,
                name: 'Alpha Project',
                role: 'OWNER',
                memberCount: 4,
                taskCount: 12,
                doneTaskCount: 5,
                progressRate: 42,
                updatedAt: '2026-03-08T10:00:00',
              },
            ],
          })
        );
      }

      if (url === '/projects/11/dashboard') {
        return Promise.reject(new Error('stats failed'));
      }

      if (url === '/notifications/unread-count') {
        return Promise.resolve(apiOk({ unreadCount: 0 }));
      }

      return Promise.reject(new Error(`unexpected url: ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <App />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    });

    expect(await screen.findByText('Failed to load project stats.')).toBeInTheDocument();
  });
});
