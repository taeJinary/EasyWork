import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import WorkspaceDetailPage from '@/pages/WorkspaceDetailPage';
import { apiOk } from '@/test/helpers';

const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
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
      )
      .mockResolvedValueOnce(
        apiOk([
          {
            projectId: 21,
            name: 'Roadmap',
            description: 'Q2 roadmap',
            role: 'OWNER',
            memberCount: 2,
            taskCount: 5,
            doneTaskCount: 2,
            progressRate: 40,
            updatedAt: '2026-03-01T12:00:00',
          },
        ])
      )
      .mockResolvedValueOnce(apiOk([
        {
          invitationId: 31,
          workspaceId: 1,
          inviteeUserId: 7,
          inviteeEmail: 'invitee@example.com',
          inviteeNickname: 'Invitee',
          role: 'MEMBER',
          status: 'PENDING',
          expiresAt: '2026-03-20T10:00:00',
          createdAt: '2026-03-09T10:00:00',
        },
      ]));

    render(
      <MemoryRouter initialEntries={['/workspaces/1']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
          <Route path="/projects/:projectId/board" element={<div>Project Board</div>} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 1, name: 'Alpha Workspace' })).toBeInTheDocument();
    });

    expect(screen.getByText('Owner')).toBeInTheDocument();
    expect(screen.getByText('Member')).toBeInTheDocument();
    expect(screen.getAllByText('멤버 2명').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Roadmap')).toBeInTheDocument();
    expect(screen.getByText('진행 중 3개')).toBeInTheDocument();
    expect(screen.getAllByText('OWNER').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('대기 중인 초대')).toBeInTheDocument();
    expect(screen.getByText('Invitee')).toBeInTheDocument();
  });

  it('skips sent invitation fetch for non-owner members', async () => {
    mockGet
      .mockResolvedValueOnce(
        apiOk({
          workspaceId: 2,
          name: 'Member Workspace',
          description: 'Shared workspace',
          myRole: 'MEMBER',
          memberCount: 3,
          updatedAt: '2026-03-01T10:00:00',
        })
      )
      .mockResolvedValueOnce(
        apiOk([
          {
            memberId: 21,
            userId: 2,
            email: 'member@example.com',
            nickname: 'Member',
            role: 'MEMBER',
            joinedAt: '2026-03-01T11:00:00',
          },
        ])
      )
      .mockResolvedValueOnce(apiOk([]));

    render(
      <MemoryRouter initialEntries={['/workspaces/2']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { level: 1, name: 'Member Workspace' })).toBeInTheDocument();
    expect(screen.queryByText('대기 중인 초대')).not.toBeInTheDocument();
    expect(mockGet).not.toHaveBeenCalledWith('/workspaces/2/invitations', { params: { status: 'PENDING' } });
  });

  it('opens new project modal and creates a project in the current workspace', async () => {
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
      .mockResolvedValueOnce(apiOk([]))
      .mockResolvedValueOnce(apiOk([]))
      .mockResolvedValueOnce(apiOk([]));
    mockPost.mockResolvedValue(
      apiOk({
        projectId: 21,
        name: 'Roadmap',
        description: 'Q2 roadmap',
        role: 'OWNER',
      })
    );

    render(
      <MemoryRouter initialEntries={['/workspaces/1']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
          <Route path="/projects/:projectId/board" element={<div>Project Board</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { level: 1, name: 'Alpha Workspace' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '새 프로젝트' }));

    expect(await screen.findByRole('heading', { name: '프로젝트 생성' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('프로젝트 이름'), { target: { value: 'Roadmap' } });
    fireEvent.change(screen.getByLabelText('설명'), { target: { value: 'Q2 roadmap' } });
    fireEvent.click(screen.getByRole('button', { name: '프로젝트 생성' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/projects', {
        workspaceId: 1,
        name: 'Roadmap',
        description: 'Q2 roadmap',
      });
    });
  });

  it('shows dedicated error state when workspace detail loading fails', async () => {
    mockGet.mockRejectedValueOnce(new Error('workspace load failed'));

    render(
      <MemoryRouter initialEntries={['/workspaces/1']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('작업공간을 불러오지 못했습니다.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument();
  });

  it('opens workspace invite modal and submits invitation with workspace contract', async () => {
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
      .mockResolvedValueOnce(apiOk([]))
      .mockResolvedValueOnce(apiOk([]))
      .mockResolvedValueOnce(apiOk([]));
    mockPost.mockResolvedValue(
      apiOk({
        invitationId: 31,
        workspaceId: 1,
        inviteeUserId: 7,
        inviteeEmail: 'member@example.com',
        inviteeNickname: 'Member',
        role: 'MEMBER',
        status: 'PENDING',
        expiresAt: '2026-03-20T10:00:00',
      })
    );

    render(
      <MemoryRouter initialEntries={['/workspaces/1']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { level: 1, name: 'Alpha Workspace' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '초대' }));

    expect(await screen.findByRole('heading', { name: '작업공간 멤버 초대' })).toBeInTheDocument();
    const dialogQueries = within(screen.getByRole('dialog'));

    fireEvent.change(dialogQueries.getByLabelText('이메일'), { target: { value: 'member@example.com' } });
    fireEvent.change(dialogQueries.getByLabelText('권한'), { target: { value: 'MEMBER' } });
    fireEvent.click(dialogQueries.getByRole('button', { name: '초대 보내기' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/workspaces/1/invitations', {
        email: 'member@example.com',
        role: 'MEMBER',
      });
    });
  });

  it('renders sent workspace invites and cancels a pending invite', async () => {
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
      .mockResolvedValueOnce(apiOk([]))
      .mockResolvedValueOnce(apiOk([]))
      .mockResolvedValueOnce(
        apiOk([
          {
            invitationId: 31,
            workspaceId: 1,
            inviteeUserId: 7,
            inviteeEmail: 'invitee@example.com',
            inviteeNickname: 'Invitee',
            role: 'MEMBER',
            status: 'PENDING',
            expiresAt: '2026-03-20T10:00:00',
            createdAt: '2026-03-09T10:00:00',
          },
        ])
      );
    mockPost.mockResolvedValueOnce(
      apiOk({
        invitationId: 31,
        workspaceId: 1,
        memberId: null,
        role: 'MEMBER',
        status: 'CANCELED',
      })
    );

    render(
      <MemoryRouter initialEntries={['/workspaces/1']}>
        <Routes>
          <Route path="/workspaces/:workspaceId" element={<WorkspaceDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Invitee')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '초대 취소' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/workspaces/1/invitations/31/cancel');
    });

    await waitFor(() => {
      expect(screen.queryByText('Invitee')).not.toBeInTheDocument();
    });
  });
});
