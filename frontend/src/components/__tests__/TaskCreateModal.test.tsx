import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TaskCreateModal from '@/components/TaskCreateModal';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
}));

describe('TaskCreateModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('resets form state when projectId changes while open', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/3/labels') {
        return Promise.resolve(
          apiOk([
            {
              labelId: 1,
              name: 'Release',
              colorHex: '#2563EB',
            },
          ])
        );
      }

      if (url === '/projects/4/labels') {
        return Promise.resolve(
          apiOk([
            {
              labelId: 2,
              name: 'Backend',
              colorHex: '#10B981',
            },
          ])
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    const onClose = vi.fn();
    const onCreated = vi.fn();
    const user = userEvent.setup();
    const view = render(<TaskCreateModal projectId={3} open onClose={onClose} onCreated={onCreated} />);

    await user.type(screen.getByLabelText('Task Title'), 'Carry Over');
    await user.type(screen.getByLabelText('Description'), 'Do not keep me');
    await user.click(await screen.findByLabelText('Release'));

    view.rerender(<TaskCreateModal projectId={4} open onClose={onClose} onCreated={onCreated} />);

    expect(screen.getByLabelText('Task Title')).toHaveValue('');
    expect(screen.getByLabelText('Description')).toHaveValue('');
    expect(screen.queryByLabelText('Release')).not.toBeInTheDocument();

    const backendCheckbox = await screen.findByLabelText('Backend');
    expect(backendCheckbox).not.toBeChecked();

    await user.type(screen.getByLabelText('Task Title'), 'Fresh task');
    await user.click(backendCheckbox);

    mockPost.mockResolvedValue(
      apiOk({
        taskId: 11,
        projectId: 4,
        title: 'Fresh task',
        status: 'TODO',
        priority: 'MEDIUM',
        position: 0,
        version: 1,
        assignee: null,
      })
    );

    await user.click(screen.getByRole('button', { name: 'Create Task' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/projects/4/tasks', {
        title: 'Fresh task',
        description: '',
        priority: 'MEDIUM',
        labelIds: [2],
      });
    });
  });
});
