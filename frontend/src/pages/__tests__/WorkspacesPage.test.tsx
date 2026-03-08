import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import WorkspacesPage from '@/pages/WorkspacesPage';
import { apiOk } from '@/test/helpers';
import type { WorkspaceListResponse } from '@/types';

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

function makeWorkspaceListResponse(): WorkspaceListResponse {
  return {
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
  };
}

describe('WorkspacesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders workspaces from paged backend response content', async () => {
    mockGet.mockResolvedValue(apiOk(makeWorkspaceListResponse()));

    render(
      <MemoryRouter>
        <WorkspacesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Core Team')).toBeInTheDocument();
    });

    expect(screen.getByText('Main workspace')).toBeInTheDocument();
    expect(screen.getByText('3 members')).toBeInTheDocument();
    expect(screen.getByText('OWNER')).toBeInTheDocument();
  });

  it('opens create modal from query param and creates a workspace', async () => {
    mockGet.mockResolvedValue(apiOk(makeWorkspaceListResponse()));
    mockPost.mockResolvedValue(
      apiOk({
        workspaceId: 2,
        name: 'New Space',
        description: 'Created from modal',
        myRole: 'OWNER',
      })
    );

    render(
      <MemoryRouter initialEntries={['/workspaces?create=workspace']}>
        <WorkspacesPage />
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { name: 'Create Workspace' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Workspace Name'), {
      target: { value: 'New Space' },
    });
    fireEvent.change(screen.getByLabelText('Description'), {
      target: { value: 'Created from modal' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Create Workspace' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/workspaces', {
        name: 'New Space',
        description: 'Created from modal',
      });
    });
  });

  it('closes create modal when clicking outside the form', async () => {
    mockGet.mockResolvedValue(apiOk(makeWorkspaceListResponse()));

    render(
      <MemoryRouter initialEntries={['/workspaces?create=workspace']}>
        <WorkspacesPage />
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { name: 'Create Workspace' })).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('workspace-create-backdrop'));

    await waitFor(() => {
      expect(screen.queryByRole('heading', { name: 'Create Workspace' })).not.toBeInTheDocument();
    });
  });
});
