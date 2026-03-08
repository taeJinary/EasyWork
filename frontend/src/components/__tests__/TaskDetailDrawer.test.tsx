import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TaskDetailDrawer from '@/components/TaskDetailDrawer';
import { apiOk } from '@/test/helpers';

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    put: vi.fn(),
  },
}));

vi.mock('@/api/client', () => ({
  default: apiMock,
}));

function stubTaskDetail() {
  return {
    taskId: 7,
    projectId: 3,
    title: 'Prepare release',
    description: 'Ship the release notes',
    status: 'TODO' as const,
    priority: 'HIGH' as const,
    dueDate: '2026-03-10',
    position: 0,
    version: 3,
    creator: { userId: 1, nickname: 'Owner' },
    assignee: { userId: 2, nickname: 'Worker' },
    labels: [{ labelId: 1, name: 'Release', colorHex: '#2563EB' }],
    commentCount: 0,
    recentStatusHistories: [],
    createdAt: '2026-03-08T10:00:00',
    updatedAt: '2026-03-08T10:00:00',
  };
}

function stubComments() {
  return {
    content: [],
    nextCursor: null,
    hasNext: false,
  };
}

describe('TaskDetailDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    apiMock.get.mockImplementation((url: string) => {
      if (url === '/tasks/7') {
        return Promise.resolve(apiOk(stubTaskDetail()));
      }

      if (url === '/tasks/7/comments') {
        return Promise.resolve(apiOk(stubComments()));
      }

      if (url === '/tasks/7/attachments') {
        return Promise.resolve(apiOk([]));
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
  });

  it('uses the version returned by move response for the next status change', async () => {
    apiMock.patch
      .mockResolvedValueOnce(
        apiOk({ taskId: 7, status: 'IN_PROGRESS', position: 0, version: 10, completedAt: null })
      )
      .mockResolvedValueOnce(
        apiOk({ taskId: 7, status: 'DONE', position: 0, version: 11, completedAt: '2026-03-08T11:00:00' })
      );

    const { container } = render(<TaskDetailDrawer taskId={7} onClose={vi.fn()} />);

    await screen.findByText('Prepare release');

    const statusSelect = container.querySelector('select');
    expect(statusSelect).not.toBeNull();

    fireEvent.change(statusSelect as HTMLSelectElement, { target: { value: 'IN_PROGRESS' } });

    await waitFor(() => {
      expect(apiMock.patch).toHaveBeenNthCalledWith(1, '/tasks/7/move', {
        toStatus: 'IN_PROGRESS',
        targetPosition: 0,
        version: 3,
      });
    });

    fireEvent.change(statusSelect as HTMLSelectElement, { target: { value: 'DONE' } });

    await waitFor(() => {
      expect(apiMock.patch).toHaveBeenNthCalledWith(2, '/tasks/7/move', {
        toStatus: 'DONE',
        targetPosition: 0,
        version: 10,
      });
    });
  });

  it('appends backend comment response and updates activity count', async () => {
    apiMock.post.mockResolvedValue(
      apiOk({
        commentId: 55,
        author: { userId: 1, nickname: 'Owner' },
        content: 'Looks good to me',
        createdAt: '2026-03-08T12:00:00',
        updatedAt: '2026-03-08T12:00:00',
        editable: true,
      })
    );

    render(<TaskDetailDrawer taskId={7} onClose={vi.fn()} />);

    await screen.findByText('Prepare release');

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText(/comment/i), 'Looks good to me');
    await user.click(screen.getByRole('button', { name: 'Comment' }));

    await waitFor(() => {
      expect(apiMock.post).toHaveBeenCalledWith('/tasks/7/comments', {
        content: 'Looks good to me',
      });
    });

    expect(await screen.findByText('Looks good to me')).toBeInTheDocument();
    expect(screen.getByText('Activity (1)')).toBeInTheDocument();
  });
});
