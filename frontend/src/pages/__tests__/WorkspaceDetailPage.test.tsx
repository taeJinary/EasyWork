import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import WorkspaceDetailPage from '@/pages/WorkspaceDetailPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ workspaceId: '1' }),
  };
});

describe('WorkspaceDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mockGet.mockImplementation((url: string) => {
      if (url === '/workspaces/1') {
        return Promise.resolve(
          apiOk({
            workspaceId: 1,
            name: 'Core Team',
            description: 'Main workspace',
            myRole: 'OWNER',
            memberCount: 2,
            updatedAt: '2026-03-08T01:00:00',
          })
        );
      }

      if (url === '/workspaces/1/members') {
        return Promise.resolve(
          apiOk([
            {
              memberId: 10,
              userId: 1,
              email: 'demo@easywork.local',
              nickname: '데모',
              role: 'OWNER',
              joinedAt: '2026-03-08T01:00:00',
            },
          ])
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
  });

  it('renders workspace members from backend nickname response without crashing', async () => {
    render(
      <MemoryRouter>
        <WorkspaceDetailPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Core Team' })).toBeInTheDocument();
    });

    expect(screen.getByText('데모')).toBeInTheDocument();
  });
});
