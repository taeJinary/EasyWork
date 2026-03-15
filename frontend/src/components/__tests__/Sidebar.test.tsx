import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Sidebar from '@/components/Sidebar';
import { useUiStore } from '@/stores/uiStore';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('Sidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGet.mockResolvedValue(
      apiOk({
        content: [],
        page: 0,
        size: 3,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      })
    );
    useUiStore.setState({ isMobileSidebarOpen: false });
  });

  it('routes Home navigation to dashboard', async () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: /home/i })).toHaveAttribute('href', '/dashboard');
    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/projects', { params: { page: 0, size: 3 } });
    });
  });

  it('renders recent projects from the projects API instead of placeholders', async () => {
    mockGet.mockResolvedValue(
      apiOk({
        content: [
          {
            projectId: 21,
            name: 'Backend Revamp',
            description: 'API cleanup',
            role: 'OWNER',
            memberCount: 4,
            taskCount: 12,
            doneTaskCount: 5,
            progressRate: 41,
            updatedAt: '2026-03-15T09:00:00',
          },
          {
            projectId: 22,
            name: 'Design Ops',
            description: 'Component updates',
            role: 'MEMBER',
            memberCount: 3,
            taskCount: 6,
            doneTaskCount: 2,
            progressRate: 33,
            updatedAt: '2026-03-15T08:00:00',
          },
          {
            projectId: 23,
            name: 'Mobile Sync',
            description: 'Push sync',
            role: 'MEMBER',
            memberCount: 5,
            taskCount: 9,
            doneTaskCount: 3,
            progressRate: 33,
            updatedAt: '2026-03-15T07:00:00',
          },
        ],
        page: 0,
        size: 3,
        totalElements: 3,
        totalPages: 1,
        first: true,
        last: true,
      })
    );

    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /backend revamp/i })).toHaveAttribute(
        'href',
        '/projects/21/board'
      );
    });

    expect(mockGet).toHaveBeenCalledWith('/projects', { params: { page: 0, size: 3 } });
    expect(screen.getByRole('link', { name: /design ops/i })).toHaveAttribute(
      'href',
      '/projects/22/board'
    );
    expect(screen.getByRole('link', { name: /mobile sync/i })).toHaveAttribute(
      'href',
      '/projects/23/board'
    );
    expect(screen.queryByText('EasyWork')).not.toBeInTheDocument();
    expect(screen.queryByText('Internal Tools')).not.toBeInTheDocument();
    expect(screen.queryByText('Design System')).not.toBeInTheDocument();
  });

  it('hides recent projects when the projects API returns no data', async () => {
    mockGet.mockResolvedValue(
      apiOk({
        content: [],
        page: 0,
        size: 3,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      })
    );

    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/projects', { params: { page: 0, size: 3 } });
    });

    expect(screen.queryByText('Recent Projects')).not.toBeInTheDocument();
  });

  it('refetches recent projects when the route changes after project creation', async () => {
    mockGet
      .mockResolvedValueOnce(
        apiOk({
          content: [],
          page: 0,
          size: 3,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
        })
      )
      .mockResolvedValueOnce(
        apiOk({
          content: [
            {
              projectId: 31,
              name: 'Realtime Sidebar',
              description: 'Recent projects sync',
              role: 'OWNER',
              memberCount: 2,
              taskCount: 4,
              doneTaskCount: 1,
              progressRate: 25,
              updatedAt: '2026-03-15T10:00:00',
            },
          ],
          page: 0,
          size: 3,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
        })
      );

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="*" element={<Sidebar />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(mockGet).toHaveBeenNthCalledWith(1, '/projects', { params: { page: 0, size: 3 } });
    });
    expect(screen.queryByText('Recent Projects')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('link', { name: /projects/i }));

    await waitFor(() => {
      expect(mockGet).toHaveBeenNthCalledWith(2, '/projects', { params: { page: 0, size: 3 } });
    });
    expect(screen.getByRole('link', { name: /realtime sidebar/i })).toHaveAttribute(
      'href',
      '/projects/31/board'
    );
  });
});
