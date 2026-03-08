import { render, screen, waitFor } from '@testing-library/react';
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
            ],
          })
        );
      }

      if (url === '/notifications/unread-count') {
        return Promise.resolve(apiOk({ unreadCount: 3 }));
      }

      return Promise.reject(new Error(`unexpected url: ${url}`));
    });
  });

  it('renders dashboard projects summary on /dashboard', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <App />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    });

    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('Alpha Project')).toBeInTheDocument();
    expect(screen.getByText('42% complete')).toBeInTheDocument();
  });
});
