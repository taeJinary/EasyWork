import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import NotificationsPage from '@/pages/NotificationsPage';
import { apiOk } from '@/test/helpers';
import type { NotificationItem, NotificationListResponse } from '@/types';

// Mock apiClient
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPatch = vi.fn();
const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
    patch: (...args: unknown[]) => mockPatch(...args),
    delete: vi.fn(),
  },
}));

function makeNotification(overrides: Partial<NotificationItem> = {}): NotificationItem {
  return {
    notificationId: 1,
    type: 'TASK_ASSIGNED',
    title: 'Task assigned to you',
    content: 'Bug fix task',
    referenceType: 'TASK',
    referenceId: 42,
    isRead: false,
    createdAt: new Date(Date.now() - 60000 * 5).toISOString(), // 5 mins ago
    ...overrides,
  };
}

function makeListResponse(items: NotificationItem[]): NotificationListResponse {
  return {
    content: items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

function makePagedListResponse(items: NotificationItem[], page: number, totalPages: number): NotificationListResponse {
  return {
    content: items,
    page,
    size: 20,
    totalElements: totalPages * 20,
    totalPages,
    first: page === 0,
    last: page === totalPages - 1,
  };
}

function setupMocks(items: NotificationItem[], unreadCount?: number) {
  mockGet.mockImplementation((url: string) => {
    if (typeof url === 'string' && url.includes('unread-count')) {
      return Promise.resolve(apiOk({ unreadCount: unreadCount ?? items.filter((n) => !n.isRead).length }));
    }
    return Promise.resolve(apiOk(makeListResponse(items)));
  });
}

function renderPage() {
  return render(
    <MemoryRouter>
      <NotificationsPage />
    </MemoryRouter>
  );
}

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders notifications from API', async () => {
    const n = makeNotification({ title: 'New comment on your task' });
    setupMocks([n]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('New comment on your task')).toBeInTheDocument();
    });
  });

  it('shows empty state when no notifications', async () => {
    setupMocks([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('알림이 없습니다.')).toBeInTheDocument();
    });
  });

  it('marks single notification as read on click', async () => {
    const n = makeNotification({ notificationId: 10, title: 'You have a task' });
    setupMocks([n]);
    mockPatch.mockResolvedValue(apiOk({ notificationId: 10, isRead: true, readAt: new Date().toISOString() }));

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('You have a task')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByText('You have a task'));

    expect(mockPatch).toHaveBeenCalledWith('/notifications/10/read');
  });

  it('read-all calls API and updates unreadCount to 0', async () => {
    const n1 = makeNotification({ notificationId: 1, title: 'Notif 1', isRead: false });
    const n2 = makeNotification({ notificationId: 2, title: 'Notif 2', isRead: false });
    setupMocks([n1, n2], 2);
    mockPost.mockResolvedValue(apiOk({ updatedCount: 2 }));

    renderPage();
    const user = userEvent.setup();

    // Wait for items to render
    await waitFor(() => {
      expect(screen.getByText('Notif 1')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument(); // unread count badge
    });

    // Click "모두 읽음"
    await user.click(screen.getByText('모두 읽음'));

    // API should be called
    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/notifications/read-all');
    });
  });

  it('displays unread badge count', async () => {
    const n = makeNotification({ isRead: false });
    setupMocks([n], 5);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument();
    });
  });

  it('shows error when fetch fails', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    mockGet.mockRejectedValue(new Error('Network Error'));

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('알림을 불러오는 데 실패했습니다.')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument();
    expect(consoleErrorSpy).not.toHaveBeenCalled();
    consoleErrorSpy.mockRestore();
  });

  it('retries notification loading after fetch failure', async () => {
    let listCallCount = 0;
    mockGet.mockImplementation((url: string) => {
      if (typeof url === 'string' && url.includes('unread-count')) {
        return Promise.resolve(apiOk({ unreadCount: 1 }));
      }

      listCallCount += 1;
      if (listCallCount === 1) {
        return Promise.reject(new Error('Network Error'));
      }

      return Promise.resolve(apiOk(makeListResponse([makeNotification({ title: '복구된 알림' })])));
    });

    renderPage();
    const user = userEvent.setup();

    expect(await screen.findByText('알림을 불러오는 데 실패했습니다.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '다시 시도' }));

    await waitFor(() => {
      expect(screen.getByText('복구된 알림')).toBeInTheDocument();
    });
  });

  it('restores the full notification item when unread-only read fails', async () => {
    const notification = makeNotification({
      notificationId: 77,
      title: 'Unread invitation',
      referenceType: 'INVITATION',
      referenceId: 77,
      isRead: false,
    });
    setupMocks([notification], 1);
    mockPatch.mockRejectedValue(new Error('Patch failed'));

    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('Unread invitation')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByText('Unread invitation'));

    await waitFor(() => {
      expect(mockPatch).toHaveBeenCalledWith('/notifications/77/read');
    });

    await waitFor(() => {
      expect(screen.getByText('Unread invitation')).toBeInTheDocument();
    });
  });

  it('navigates workspace invitation notifications to the workspace invitation tab', async () => {
    const notification = makeNotification({
      notificationId: 90,
      title: 'Workspace invitation',
      referenceType: 'WORKSPACE_INVITATION',
      referenceId: 13,
      isRead: true,
    });
    setupMocks([notification], 0);

    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('Workspace invitation')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Workspace invitation'));

    expect(mockNavigate).toHaveBeenCalledWith('/invitations?kind=workspace');
  });

  it('renders Korean pagination labels', async () => {
    const notifications = Array.from({ length: 2 }, (_, index) =>
      makeNotification({ notificationId: index + 1, title: `알림 ${index + 1}` })
    );
    mockGet.mockImplementation((url: string) => {
      if (typeof url === 'string' && url.includes('unread-count')) {
        return Promise.resolve(apiOk({ unreadCount: 2 }));
      }

      return Promise.resolve(apiOk(makePagedListResponse(notifications, 0, 2)));
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('페이지 1 / 2')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /이전/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /다음/i })).toBeInTheDocument();
  });

});
