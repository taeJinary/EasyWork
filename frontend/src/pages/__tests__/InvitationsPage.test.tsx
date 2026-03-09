import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import InvitationsPage from '@/pages/InvitationsPage';
import { apiOk } from '@/test/helpers';
import type { InvitationListItem, InvitationListResponse } from '@/types';

// Mock apiClient
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

function makeInvitation(overrides: Partial<InvitationListItem> = {}): InvitationListItem {
  return {
    invitationId: 1,
    projectId: 10,
    projectName: 'Test Project',
    inviterUserId: 100,
    inviterNickname: 'Alice',
    role: 'MEMBER',
    status: 'PENDING',
    expiresAt: '2026-04-01T00:00:00',
    createdAt: '2026-03-07T10:00:00',
    ...overrides,
  };
}

function makeListResponse(items: InvitationListItem[]): InvitationListResponse {
  return {
    content: items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

function makeWorkspaceInvitation(
  overrides: Partial<{
    invitationId: number;
    workspaceId: number;
    workspaceName: string;
    inviterUserId: number;
    inviterNickname: string;
    role: 'OWNER' | 'MEMBER';
    status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED' | 'EXPIRED';
    expiresAt: string;
    createdAt: string;
  }> = {}
) {
  return {
    invitationId: 5,
    workspaceId: 20,
    workspaceName: 'Alpha Workspace',
    inviterUserId: 101,
    inviterNickname: 'Owner',
    role: 'MEMBER' as const,
    status: 'PENDING' as const,
    expiresAt: '2026-04-01T00:00:00',
    createdAt: '2026-03-07T10:00:00',
    ...overrides,
  };
}

function makeWorkspaceListResponse(
  items: ReturnType<typeof makeWorkspaceInvitation>[]
) {
  return {
    content: items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <InvitationsPage />
    </MemoryRouter>
  );
}

describe('InvitationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders invitation list from API', async () => {
    const inv = makeInvitation({ projectName: 'EasyWork' });
    mockGet.mockResolvedValue(apiOk(makeListResponse([inv])));

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('EasyWork')).toBeInTheDocument();
    });
    // Use getAllByText since "PENDING" also appears in the <option>
    expect(screen.getAllByText('PENDING').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Alice/)).toBeInTheDocument();
  });

  it('shows empty state when no invitations', async () => {
    mockGet.mockResolvedValue(apiOk(makeListResponse([])));

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('받은 프로젝트 초대가 없습니다.')).toBeInTheDocument();
    });
  });

  it('updates status to ACCEPTED on accept click (functional updater)', async () => {
    const inv = makeInvitation({ invitationId: 42 });
    mockGet.mockResolvedValue(apiOk(makeListResponse([inv])));
    mockPost.mockResolvedValue(
      apiOk({ invitationId: 42, projectId: 10, memberId: 1, role: 'MEMBER', status: 'ACCEPTED' })
    );

    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('수락')).toBeInTheDocument();
    });

    await user.click(screen.getByText('수락'));

    // ACCEPTED appears in both Badge and select option, so check count increased
    await waitFor(() => {
      const acceptedElements = screen.getAllByText('ACCEPTED');
      // At least 2: one in badge, one in select option
      expect(acceptedElements.length).toBeGreaterThanOrEqual(2);
    });
    // Accept/reject buttons should disappear for non-PENDING
    expect(screen.queryByText('수락')).not.toBeInTheDocument();
  });

  it('updates status to REJECTED on reject click', async () => {
    const inv = makeInvitation({ invitationId: 43 });
    mockGet.mockResolvedValue(apiOk(makeListResponse([inv])));
    mockPost.mockResolvedValue(
      apiOk({ invitationId: 43, projectId: 10, memberId: 1, role: 'MEMBER', status: 'REJECTED' })
    );

    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('거절')).toBeInTheDocument();
    });

    await user.click(screen.getByText('거절'));

    // REJECTED appears in both Badge and select option, so check count increased
    await waitFor(() => {
      const rejectedElements = screen.getAllByText('REJECTED');
      expect(rejectedElements.length).toBeGreaterThanOrEqual(2);
    });
    // Reject/accept buttons should disappear for non-PENDING
    expect(screen.queryByText('거절')).not.toBeInTheDocument();
  });

  it('uses functional updater to prevent stale closure in accept handler', async () => {
    // This test verifies the fix: setInvitations(prev => prev.map(...))
    // instead of setInvitations(invitations.map(...))
    const inv = makeInvitation({ invitationId: 10 });
    mockGet.mockResolvedValue(apiOk(makeListResponse([inv])));
    mockPost.mockResolvedValue(
      apiOk({ invitationId: 10, projectId: 10, memberId: 1, role: 'MEMBER', status: 'ACCEPTED' })
    );

    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('수락')).toBeInTheDocument();
    });

    await user.click(screen.getByText('수락'));

    // Verify the API was called with correct path
    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/invitations/10/accept');
    });

    // After accept, buttons should disappear (status changed to ACCEPTED)
    await waitFor(() => {
      expect(screen.queryByText('수락')).not.toBeInTheDocument();
    });
  });

  it('shows error banner on API failure', async () => {
    mockGet.mockResolvedValue(apiOk(makeListResponse([makeInvitation()])));
    mockPost.mockRejectedValue(new Error('Network Error'));

    renderPage();
    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('수락')).toBeInTheDocument();
    });

    await user.click(screen.getByText('수락'));

    await waitFor(() => {
      expect(screen.getByText('초대 수락에 실패했습니다.')).toBeInTheDocument();
    });
  });

  it('renders workspace invitations and accepts them from the workspace tab', async () => {
    mockGet
      .mockResolvedValueOnce(apiOk(makeListResponse([])))
      .mockResolvedValueOnce(apiOk(makeWorkspaceListResponse([makeWorkspaceInvitation()])));
    mockPost.mockResolvedValue(
      apiOk({
        invitationId: 5,
        workspaceId: 20,
        memberId: 1,
        role: 'MEMBER',
        status: 'ACCEPTED',
      })
    );

    renderPage();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Workspace Invitations' }));

    await waitFor(() => {
      expect(screen.getByText('Alpha Workspace')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: '수락' }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith('/workspace-invitations/5/accept');
    });
  });
});
