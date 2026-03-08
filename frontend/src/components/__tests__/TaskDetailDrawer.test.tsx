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

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: {
      userId: 7,
      email: 'owner@easywork.local',
      nickname: 'Owner',
      profileImg: null,
      role: 'USER',
    },
  }),
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

function stubAttachments() {
  return [
    {
      attachmentId: 101,
      taskId: 7,
      originalFilename: 'release-notes.pdf',
      contentType: 'application/pdf',
      sizeBytes: 4096,
      uploaderUserId: 7,
      uploaderNickname: 'Owner',
      createdAt: '2026-03-08T10:30:00',
    },
  ];
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
        return Promise.resolve(apiOk(stubAttachments()));
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

  it('deletes an owned attachment and removes it from the list', async () => {
    apiMock.delete.mockResolvedValue(apiOk(null));

    render(<TaskDetailDrawer taskId={7} onClose={vi.fn()} />);

    await screen.findByText('release-notes.pdf');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Delete attachment release-notes.pdf' }));

    await waitFor(() => {
      expect(apiMock.delete).toHaveBeenCalledWith('/attachments/101');
    });

    await waitFor(() => {
      expect(screen.queryByText('release-notes.pdf')).not.toBeInTheDocument();
    });
  });

  it('edits task details and notifies parent after save', async () => {
    const onTaskUpdated = vi.fn();
    apiMock.patch.mockResolvedValue(
      apiOk({
        ...stubTaskDetail(),
        title: 'Prepare final release',
        description: 'Ship the final release notes',
        version: 4,
      })
    );

    render(<TaskDetailDrawer taskId={7} onClose={vi.fn()} onTaskUpdated={onTaskUpdated} />);

    await screen.findByText('Prepare release');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Edit task' }));

    await user.clear(screen.getByLabelText('Task Title'));
    await user.type(screen.getByLabelText('Task Title'), 'Prepare final release');
    await user.clear(screen.getByLabelText('Task Description'));
    await user.type(screen.getByLabelText('Task Description'), 'Ship the final release notes');
    await user.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(apiMock.patch).toHaveBeenCalledWith('/tasks/7', {
        title: 'Prepare final release',
        description: 'Ship the final release notes',
        assigneeUserId: 2,
        priority: 'HIGH',
        dueDate: '2026-03-10',
        labelIds: [1],
        version: 3,
      });
    });

    expect(await screen.findByText('Prepare final release')).toBeInTheDocument();
    expect(onTaskUpdated).toHaveBeenCalledTimes(1);
  });

  it('deletes task after confirmation and closes drawer', async () => {
    const onClose = vi.fn();
    const onTaskDeleted = vi.fn();
    apiMock.delete.mockResolvedValue(apiOk(null));
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(
      <TaskDetailDrawer taskId={7} onClose={onClose} onTaskDeleted={onTaskDeleted} />
    );

    await screen.findByText('Prepare release');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Delete task' }));

    await waitFor(() => {
      expect(apiMock.delete).toHaveBeenCalledWith('/tasks/7');
    });

    expect(onTaskDeleted).toHaveBeenCalledWith(7);
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
