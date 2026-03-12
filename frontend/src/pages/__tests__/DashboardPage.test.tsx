import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '@/App';
import { useAuthStore } from '@/stores/authStore';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

function deferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });

  return { promise, resolve, reject };
}

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
      expect(screen.getByRole('heading', { name: '대시보드' })).toBeInTheDocument();
    });

    expect(screen.getAllByText('Alpha Project').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Beta Project').length).toBeGreaterThan(0);
    expect(screen.getByText('완료율 42%')).toBeInTheDocument();
    expect(screen.getByText('프로젝트 통계')).toBeInTheDocument();
    expect(await screen.findByText('기한 초과')).toBeInTheDocument();
    expect(screen.getByText('마감 임박')).toBeInTheDocument();

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/projects/11/dashboard');
    });

    const user = userEvent.setup();
    const viewStatsButtons = screen.getAllByRole('button', { name: '통계 보기' });
    await user.click(viewStatsButtons[1]);

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/projects/12/dashboard');
    });

    expect(screen.getAllByText('Beta Project').length).toBeGreaterThan(0);
    expect(screen.getByText('2/9')).toBeInTheDocument();
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
      expect(screen.getByRole('heading', { name: '대시보드' })).toBeInTheDocument();
    });

    expect(await screen.findByText('프로젝트 통계를 불러오지 못했습니다.')).toBeInTheDocument();
  });

  it('retries dashboard fetch after initial load failure', async () => {
    let dashboardAttempts = 0;

    mockGet.mockImplementation((url: string) => {
      if (url === '/dashboard/projects') {
        dashboardAttempts += 1;

        if (dashboardAttempts === 1) {
          return Promise.reject(new Error('dashboard failed'));
        }

        return Promise.resolve(
          apiOk({
            pendingInvitationCount: 0,
            myProjects: [
              {
                projectId: 21,
                name: 'Gamma Project',
                role: 'OWNER',
                memberCount: 3,
                taskCount: 7,
                doneTaskCount: 1,
                progressRate: 14,
                updatedAt: '2026-03-08T12:00:00',
              },
            ],
          })
        );
      }

      if (url === '/projects/21/dashboard') {
        return Promise.resolve(
          apiOk({
            projectId: 21,
            memberCount: 3,
            taskCount: 7,
            todoCount: 4,
            inProgressCount: 2,
            doneCount: 1,
            overdueCount: 0,
            dueSoonCount: 1,
            completionRate: 14,
          })
        );
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

    expect(await screen.findByText('대시보드 데이터를 불러오지 못했습니다.')).toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '다시 시도' }));

    expect((await screen.findAllByText('Gamma Project')).length).toBeGreaterThan(0);
    expect(mockGet).toHaveBeenCalledWith('/dashboard/projects');
  });

  it('shows a guided empty state when there are no active projects', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/dashboard/projects') {
        return Promise.resolve(
          apiOk({
            pendingInvitationCount: 0,
            myProjects: [],
          })
        );
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

    expect(await screen.findByText('아직 참여 중인 프로젝트가 없습니다.')).toBeInTheDocument();
    expect(screen.getByText('프로젝트를 만들거나 초대를 수락하면 진행 현황을 확인할 수 있습니다.')).toBeInTheDocument();
  });

  it('retries project stats fetch when the selected project stats request fails', async () => {
    let projectStatsAttempts = 0;

    mockGet.mockImplementation((url: string) => {
      if (url === '/dashboard/projects') {
        return Promise.resolve(
          apiOk({
            pendingInvitationCount: 0,
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
        projectStatsAttempts += 1;

        if (projectStatsAttempts === 1) {
          return Promise.reject(new Error('stats failed'));
        }

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

    expect(await screen.findByText('프로젝트 통계를 불러오지 못했습니다.')).toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '다시 시도' }));

    expect(await screen.findByText('5/12')).toBeInTheDocument();
    expect(mockGet).toHaveBeenCalledWith('/projects/11/dashboard');
  });

  it('ignores stale project stats responses after switching to another project', async () => {
    const alphaStats = deferred<ReturnType<typeof apiOk>>();

    mockGet.mockImplementation((url: string) => {
      if (url === '/dashboard/projects') {
        return Promise.resolve(
          apiOk({
            pendingInvitationCount: 0,
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
        return alphaStats.promise;
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
        return Promise.resolve(apiOk({ unreadCount: 0 }));
      }

      return Promise.reject(new Error(`unexpected url: ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <App />
      </MemoryRouter>
    );

    await screen.findByRole('heading', { name: '대시보드' });

    const user = userEvent.setup();
    await user.click(screen.getAllByRole('button', { name: '통계 보기' })[1]);

    expect(await screen.findByText('2/9')).toBeInTheDocument();

    alphaStats.resolve(
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

    await waitFor(() => {
      expect(screen.getAllByText('Beta Project').length).toBeGreaterThan(0);
    });
    expect(screen.getByText('2/9')).toBeInTheDocument();
    expect(screen.queryByText('5/12')).not.toBeInTheDocument();
  });
});
