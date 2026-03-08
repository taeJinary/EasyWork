import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import WorkspacesPage from '@/pages/WorkspacesPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('WorkspacesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders workspace list from backend paged content contract', async () => {
    mockGet.mockResolvedValue(
      apiOk({
        content: [
          {
            workspaceId: 1,
            name: 'Alpha Workspace',
            description: 'Main workspace',
            myRole: 'OWNER',
            memberCount: 3,
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
        <WorkspacesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alpha Workspace')).toBeInTheDocument();
    });

    expect(screen.getByText('Main workspace')).toBeInTheDocument();
    expect(screen.getByText('3 members')).toBeInTheDocument();
    expect(screen.getByText('OWNER')).toBeInTheDocument();
  });
});
