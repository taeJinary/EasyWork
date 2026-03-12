import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProjectMembersPage from '@/pages/ProjectMembersPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    patch: vi.fn(),
    delete: vi.fn(),
    post: vi.fn(),
  },
}));

describe('ProjectMembersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders translated member list copy', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/3') {
        return Promise.resolve(
          apiOk({
            projectId: 3,
            name: 'Release Project',
            description: 'Team page',
            myRole: 'OWNER',
            memberCount: 2,
            pendingInvitationCount: 0,
            taskSummary: {
              todo: 1,
              inProgress: 0,
              done: 0,
            },
            members: [],
          })
        );
      }

      if (url === '/projects/3/members') {
        return Promise.resolve(
          apiOk([
            {
              memberId: 11,
              userId: 1,
              nickname: 'Owner',
              email: 'owner@example.com',
              role: 'OWNER',
              joinedAt: '2026-03-10T10:00:00',
            },
          ])
        );
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/projects/3/members']}>
        <Routes>
          <Route path="/projects/:projectId/members" element={<ProjectMembersPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '멤버 초대' })).toBeInTheDocument();
    });

    expect(screen.getByText('멤버 1명')).toBeInTheDocument();
    expect(screen.getByText('owner@example.com - 참여일 2026.03.10')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '보드' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '목록' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '멤버' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '설정' })).toBeInTheDocument();
  });

  it('shows translated invite modal copy', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/projects/3') {
        return Promise.resolve(
          apiOk({
            projectId: 3,
            name: 'Release Project',
            description: 'Team page',
            myRole: 'OWNER',
            memberCount: 0,
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

      if (url === '/projects/3/members') {
        return Promise.resolve(apiOk([]));
      }

      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/projects/3/members']}>
        <Routes>
          <Route path="/projects/:projectId/members" element={<ProjectMembersPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('button', { name: '멤버 초대' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '멤버 초대' }));

    expect(await screen.findByRole('heading', { name: '멤버 초대' })).toBeInTheDocument();
    expect(screen.getByLabelText('이메일')).toBeInTheDocument();
    expect(screen.getByLabelText('권한')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '취소' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '초대 보내기' })).toBeInTheDocument();
  });
});
