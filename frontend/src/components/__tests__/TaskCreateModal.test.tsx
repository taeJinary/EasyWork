import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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
    const view = render(<TaskCreateModal projectId={3} open onClose={onClose} onCreated={onCreated} />);

    fireEvent.change(screen.getByLabelText('작업 제목'), { target: { value: 'Carry Over' } });
    fireEvent.change(screen.getByLabelText('설명'), { target: { value: 'Do not keep me' } });
    fireEvent.click(await screen.findByLabelText('Release'));

    view.rerender(<TaskCreateModal projectId={4} open onClose={onClose} onCreated={onCreated} />);

    expect(screen.getByLabelText('작업 제목')).toHaveValue('');
    expect(screen.getByLabelText('설명')).toHaveValue('');
    expect(screen.queryByLabelText('Release')).not.toBeInTheDocument();

    const backendCheckbox = await screen.findByLabelText('Backend');
    expect(backendCheckbox).not.toBeChecked();

    fireEvent.change(screen.getByLabelText('작업 제목'), { target: { value: 'Fresh task' } });
    fireEvent.click(backendCheckbox);

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

    fireEvent.click(screen.getByRole('button', { name: '작업 생성' }));

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
