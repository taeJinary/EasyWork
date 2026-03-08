import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProjectsPage from '@/pages/ProjectsPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('ProjectsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders project list from backend paged content contract', async () => {
    mockGet.mockResolvedValue(
      apiOk({
        content: [
          {
            projectId: 10,
            name: 'Alpha Project',
            description: 'Main project',
            role: 'OWNER',
            memberCount: 4,
            taskCount: 9,
            doneTaskCount: 3,
            progressRate: 33,
            updatedAt: '2026-03-01T10:00:00',
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

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Project')).toBeInTheDocument();
    });

    expect(screen.getByText('Main project')).toBeInTheDocument();
    expect(screen.getByText('OWNER')).toBeInTheDocument();
    expect(screen.getByText('4 members')).toBeInTheDocument();
    expect(screen.getByText('6 open tasks')).toBeInTheDocument();
  });
});
