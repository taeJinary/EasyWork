import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import WorkspaceDetailPage from '@/pages/WorkspaceDetailPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe('WorkspaceDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders members from backend workspace detail contracts without crashing', async () => {
    mockGet
      .mockResolvedValueOnce(
        apiOk({
          workspaceId: 1,
          name: 'Alpha Workspace',
          description: 'Main workspace',
          myRole: 'OWNER',
          memberCount: 2,
          updatedAt: '2026-03-01T10:00:00',
        })
      )
      .mockResolvedValueOnce(
        apiOk([
          {
            memberId: 11,
            userId: 1,
            email: 'owner@example.com',
            nickname: 'Owner',
            role: 'OWNER',
            joinedAt: '2026-03-01T10:00:00',
          },
          {
            memberId: 12,
            userId: 2,
            email: 'member@example.com',
            nickname: 'Member',
            role: 'MEMBER',
            joinedAt: '2026-03-01T11:00:00',
          },
        ])
      );

    render(
      <MemoryRouter initialEntries={['/workspaces/1']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 1, name: 'Alpha Workspace' })).toBeInTheDocument();
    });

    expect(screen.getByText('Owner')).toBeInTheDocument();
    expect(screen.getByText('Member')).toBeInTheDocument();
    expect(screen.getByText('2 members')).toBeInTheDocument();
    expect(screen.getAllByText('OWNER').length).toBeGreaterThanOrEqual(1);
  });
});
