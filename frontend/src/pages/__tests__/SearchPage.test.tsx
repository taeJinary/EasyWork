import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SearchPage from '@/pages/SearchPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('SearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders matching projects and workspaces from API-backed search data', async () => {
    mockGet.mockImplementation((url: string, config?: { params?: Record<string, unknown> }) => {
      if (url === '/projects') {
        expect(config).toEqual({ params: { page: 0, size: 20, keyword: 'dd' } });
        return Promise.resolve(
          apiOk({
            content: [
              {
                projectId: 10,
                name: 'dd project',
                description: 'target project',
                role: 'OWNER',
                memberCount: 2,
                taskCount: 3,
                doneTaskCount: 1,
                progressRate: 33,
                updatedAt: '2026-03-15T09:00:00',
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

      if (url === '/workspaces') {
        return Promise.resolve(
          apiOk({
            content: [
              {
                workspaceId: 2,
                name: 'dd workspace',
                description: 'target workspace',
                myRole: 'OWNER',
                memberCount: 4,
                updatedAt: '2026-03-15T08:00:00',
              },
              {
                workspaceId: 3,
                name: 'alpha',
                description: 'other workspace',
                myRole: 'MEMBER',
                memberCount: 2,
                updatedAt: '2026-03-15T07:00:00',
              },
            ],
            page: 0,
            size: 20,
            totalElements: 2,
            totalPages: 1,
            first: true,
            last: true,
          })
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/search?q=dd']}>
        <SearchPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('dd project')).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: 'dd project' })).toHaveAttribute('href', '/projects/10/board');
    expect(screen.getByRole('link', { name: 'dd workspace' })).toHaveAttribute('href', '/workspaces/2');
    expect(screen.queryByText('alpha')).not.toBeInTheDocument();
  });
});
