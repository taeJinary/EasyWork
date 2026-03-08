import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProjectsPage from '@/pages/ProjectsPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
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

  it('opens project modal, loads workspaces, and creates a project', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects') {
        return Promise.resolve(
          apiOk({
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 1,
            first: true,
            last: true,
          })
        );
      }

      if (url === '/workspaces') {
        return Promise.resolve(
          apiOk({
            content: [
              {
                workspaceId: 1,
                name: 'Core Team',
                description: 'Main workspace',
                myRole: 'OWNER',
                memberCount: 3,
                updatedAt: '2026-03-08T01:00:00',
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
    mockPost.mockResolvedValue(
      apiOk({
        projectId: 11,
        name: 'Roadmap',
        description: 'Q2 roadmap',
        role: 'OWNER',
      })
    );

    render(
      <MemoryRouter>
        <ProjectsPage />
      </MemoryRouter>
    );

    expect(await screen.findByRole('button', { name: 'New Project' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'New Project' }));

    expect(await screen.findByRole('heading', { name: 'Create Project' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Workspace'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Project Name'), { target: { value: 'Roadmap' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Q2 roadmap' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Project' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/projects', {
        workspaceId: 1,
        name: 'Roadmap',
        description: 'Q2 roadmap',
      });
    });
  });
});
