import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TaskListPage from '@/pages/TaskListPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('TaskListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders task list from backend paged content contract', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/3') {
        return Promise.resolve(
          apiOk({
            projectId: 3,
            name: 'Release Project',
            description: 'Ship the release safely',
            myRole: 'OWNER',
            memberCount: 4,
            pendingInvitationCount: 1,
            taskSummary: {
              todo: 3,
              inProgress: 1,
              done: 2,
            },
            members: [],
          })
        );
      }

      if (url === '/projects/3/tasks') {
        return Promise.resolve(
          apiOk({
            content: [
              {
                taskId: 7,
                title: 'Prepare release notes',
                status: 'IN_PROGRESS',
                priority: 'HIGH',
                dueDate: '2026-03-12',
                position: 0,
                version: 5,
                commentCount: 2,
                assignee: {
                  userId: 11,
                  nickname: 'Worker',
                },
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
      <MemoryRouter initialEntries={['/projects/3/tasks']}>
        <Routes>
          <Route path="/projects/:projectId/tasks" element={<TaskListPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Prepare release notes')).toBeInTheDocument();
    });

    expect(screen.getByRole('heading', { name: 'Release Project' })).toBeInTheDocument();
    expect(screen.getByText('Ship the release safely')).toBeInTheDocument();
    expect(screen.getByText('TASK-7')).toBeInTheDocument();
    expect(screen.getByText('Worker')).toBeInTheDocument();
    expect(screen.getByText('Mar 12')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});
