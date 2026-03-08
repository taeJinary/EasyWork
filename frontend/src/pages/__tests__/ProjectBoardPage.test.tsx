import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import ProjectBoardPage from '@/pages/ProjectBoardPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

function LocationProbe() {
  const location = useLocation();
  return <div>{`${location.pathname}${location.search}`}</div>;
}

describe('ProjectBoardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders backend board columns and normalizes missing statuses', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/3') {
        return Promise.resolve(
          apiOk({
            projectId: 3,
            name: 'Release Project',
            description: 'Board contract',
            myRole: 'OWNER',
            memberCount: 4,
            pendingInvitationCount: 0,
            taskSummary: {
              todo: 0,
              inProgress: 0,
              done: 1,
            },
            members: [],
          })
        );
      }

      if (url === '/projects/3/tasks/board') {
        return Promise.resolve(
          apiOk({
            projectId: 3,
            filters: {
              assigneeUserId: null,
              priority: null,
              labelId: null,
              keyword: null,
            },
            columns: [
              {
                status: 'DONE',
                tasks: [
                  {
                    taskId: 91,
                    title: 'Ship release',
                    priority: 'URGENT',
                    dueDate: '2026-03-15',
                    position: 0,
                    version: 8,
                    assignee: {
                      userId: 2,
                      nickname: 'Owner',
                    },
                    labels: [
                      {
                        labelId: 1,
                        name: 'Release',
                        colorHex: '#2563EB',
                      },
                    ],
                    commentCount: 4,
                  },
                ],
              },
            ],
          })
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/projects/3/board']}>
        <Routes>
          <Route path="/projects/:projectId/board" element={<ProjectBoardPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Ship release')).toBeInTheDocument();
    });

    expect(screen.getByRole('heading', { name: 'Release Project' })).toBeInTheDocument();
    expect(screen.getByText('TODO')).toBeInTheDocument();
    expect(screen.getByText('IN PROGRESS')).toBeInTheDocument();
    expect(screen.getByText('DONE')).toBeInTheDocument();
    expect(screen.getByText('Release')).toBeInTheDocument();
    expect(screen.getByText('Owner')).toBeInTheDocument();
    expect(screen.getByText('Mar 15')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
  });

  it('navigates invite action to members page with invite query', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/3') {
        return Promise.resolve(
          apiOk({
            projectId: 3,
            name: 'Release Project',
            description: 'Board contract',
            myRole: 'OWNER',
            memberCount: 4,
            pendingInvitationCount: 0,
            taskSummary: {
              todo: 0,
              inProgress: 0,
              done: 0,
            },
            members: [],
          })
        );
      }

      if (url === '/projects/3/tasks/board') {
        return Promise.resolve(apiOk({ projectId: 3, filters: {}, columns: [] }));
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/projects/3/board']}>
        <Routes>
          <Route path="/projects/:projectId/board" element={<ProjectBoardPage />} />
          <Route path="/projects/:projectId/members" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('button', { name: 'Invite' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Invite' }));

    await waitFor(() => {
      expect(screen.getByText('/projects/3/members?invite=1')).toBeInTheDocument();
    });
  });
});
